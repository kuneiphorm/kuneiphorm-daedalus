package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.core.Expression;

class DeterminizerTest {

  // --- DFA simulation helper ---

  private static <S, L> boolean dfaAccepts(Automaton<S, L> dfa, List<L> input) {
    var current = dfa.getInitial();
    for (var symbol : input) {
      State<S, L> next = null;
      for (var t : current.getTransitions()) {
        if (symbol.equals(t.label())) {
          next = t.target();
          break;
        }
      }
      if (next == null) return false;
      current = next;
    }
    return current.isAccepting();
  }

  private static <S, L> Automaton<S, L> buildDfa(Expression<L> expr, S output) {
    return Determinizer.determinize(ExpressionConverter.build(expr, output));
  }

  // --- Null checks ---

  @Test
  void determinize_nullNfa_throwsNpe() {
    assertThrows(NullPointerException.class, () -> Determinizer.determinize(null));
  }

  @Test
  void determinize_nullPartitioner_throwsNpe() {
    var nfa = ExpressionConverter.build(Expression.unit('a'), "TOKEN");
    assertThrows(
        NullPointerException.class,
        () -> Determinizer.determinize(nfa, null, outputs -> outputs.iterator().next()));
  }

  @Test
  void determinize_nullOutputResolver_throwsNpe() {
    var nfa = ExpressionConverter.build(Expression.unit('a'), "TOKEN");
    assertThrows(
        NullPointerException.class,
        () -> Determinizer.determinize(nfa, Determinizer.byEquality(), null));
  }

  // --- Structural ---

  @Test
  void initialStateIsSet() {
    var dfa = buildDfa(Expression.unit('a'), "TOKEN");
    assertDoesNotThrow(dfa::getInitial);
  }

  @Test
  void noEpsilonTransitions() {
    var dfa = buildDfa(Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    for (var state : dfa.getStates()) {
      for (var t : state.getTransitions()) {
        assertFalse(t.isEpsilon(), "DFA must not contain epsilon transitions");
      }
    }
  }

  @Test
  void deterministicTransitions() {
    var dfa = buildDfa(Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    for (var state : dfa.getStates()) {
      var labels = state.getTransitions().stream().map(t -> t.label()).toList();
      assertEquals(
          labels.size(),
          labels.stream().distinct().count(),
          "DFA must not have duplicate transition labels from the same state");
    }
  }

  // --- Unit ---

  @Test
  void unit_acceptsMatchingChar() {
    var dfa = buildDfa(Expression.unit('a'), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a')));
  }

  @Test
  void unit_rejectsOtherChar() {
    var dfa = buildDfa(Expression.unit('a'), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of('b')));
  }

  @Test
  void unit_rejectsEpsilon() {
    var dfa = buildDfa(Expression.unit('a'), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of()));
  }

  // --- Sequence ---

  @Test
  void sequence_acceptsMatchingPair() {
    var dfa = buildDfa(Expression.sequence(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a', 'b')));
  }

  @Test
  void sequence_rejectsPartial() {
    var dfa = buildDfa(Expression.sequence(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of('a')));
    assertFalse(dfaAccepts(dfa, List.of('b')));
  }

  @Test
  void sequence_rejectsWrongOrder() {
    var dfa = buildDfa(Expression.sequence(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of('b', 'a')));
  }

  // --- Choice ---

  @Test
  void choice_acceptsBothAlternatives() {
    var dfa = buildDfa(Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a')));
    assertTrue(dfaAccepts(dfa, List.of('b')));
  }

  @Test
  void choice_rejectsOther() {
    var dfa = buildDfa(Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of('c')));
  }

  // --- Optional ---

  @Test
  void optional_acceptsEpsilon() {
    var dfa = buildDfa(Expression.optional(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of()));
  }

  @Test
  void optional_acceptsOne() {
    var dfa = buildDfa(Expression.optional(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a')));
  }

  @Test
  void optional_rejectsMore() {
    var dfa = buildDfa(Expression.optional(Expression.unit('a')), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of('a', 'a')));
  }

  // --- Star ---

  @Test
  void star_acceptsEpsilon() {
    var dfa = buildDfa(Expression.star(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of()));
  }

  @Test
  void star_acceptsOne() {
    var dfa = buildDfa(Expression.star(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a')));
  }

  @Test
  void star_acceptsMany() {
    var dfa = buildDfa(Expression.star(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a', 'a', 'a')));
  }

  @Test
  void star_rejectsWrongChar() {
    var dfa = buildDfa(Expression.star(Expression.unit('a')), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of('b')));
  }

  // --- Plus ---

  @Test
  void plus_rejectsEpsilon() {
    var dfa = buildDfa(Expression.plus(Expression.unit('a')), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of()));
  }

  @Test
  void plus_acceptsOne() {
    var dfa = buildDfa(Expression.plus(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a')));
  }

  @Test
  void plus_acceptsMany() {
    var dfa = buildDfa(Expression.plus(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(dfa, List.of('a', 'a', 'a')));
  }

  @Test
  void plus_rejectsWrongChar() {
    var dfa = buildDfa(Expression.plus(Expression.unit('a')), "TOKEN");
    assertFalse(dfaAccepts(dfa, List.of('b')));
  }

  // -------------------------------------------------------------------------
  // TransitionPartitioner
  // -------------------------------------------------------------------------

  @Test
  void byEquality_producesEquivalentDfa() {
    // Explicit byEquality() call must produce same acceptance as the convenience overload.
    var nfa =
        ExpressionConverter.build(
            Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    var dfaConvenience = Determinizer.determinize(nfa);
    var dfaExplicit =
        Determinizer.determinize(
            nfa, Determinizer.byEquality(), outputs -> outputs.iterator().next());
    // Both must accept 'a' and 'b' and reject 'c'.
    assertTrue(dfaAccepts(dfaConvenience, List.of('a')));
    assertTrue(dfaAccepts(dfaExplicit, List.of('a')));
    assertTrue(dfaAccepts(dfaConvenience, List.of('b')));
    assertTrue(dfaAccepts(dfaExplicit, List.of('b')));
    assertFalse(dfaAccepts(dfaConvenience, List.of('c')));
    assertFalse(dfaAccepts(dfaExplicit, List.of('c')));
  }

  // -------------------------------------------------------------------------
  // OutputResolver
  // -------------------------------------------------------------------------

  @Test
  void outputResolver_receivesAllCandidateOutputs() {
    // Two NFA transitions on the same label lead to different accepting states.
    // byEquality() groups them into one target kernel; resolver receives both outputs.
    var nfa = Automaton.<String, Character>create();
    var root = nfa.newState();
    nfa.setInitialStateId(root.getId());
    root.addTransition('x', nfa.newState("A"));
    root.addTransition('x', nfa.newState("B"));

    int[] receivedSize = {-1};
    Determinizer.determinize(
        nfa,
        Determinizer.byEquality(),
        outputs -> {
          receivedSize[0] = outputs.size();
          return outputs.iterator().next();
        });
    assertEquals(2, receivedSize[0]);
  }

  @Test
  void outputResolver_notCalledForNonAcceptingKernels() {
    // NFA: initial → intermediate (non-accepting) → accepting.
    var nfa = Automaton.<String, Character>create();
    var root = nfa.newState();
    nfa.setInitialStateId(root.getId());
    var mid = nfa.newState();
    var accept = nfa.newState("TOKEN");
    root.addTransition('a', mid);
    mid.addTransition('b', accept);

    int[] callCount = {0};
    Determinizer.determinize(
        nfa,
        Determinizer.byEquality(),
        outputs -> {
          callCount[0]++;
          return outputs.iterator().next();
        });
    // Resolver called exactly once -- only for the kernel containing the accepting state.
    assertEquals(1, callCount[0]);
  }

  @Test
  void outputResolver_throwingPropagatesException() {
    // Resolver that rejects ambiguous kernels -- exception must propagate out of determinize.
    var nfa = Automaton.<String, Character>create();
    var root = nfa.newState();
    nfa.setInitialStateId(root.getId());
    var a = nfa.newState("CONFLICT_A");
    var b = nfa.newState("CONFLICT_B");
    root.addTransition('x', a);
    root.addTransition('x', b);

    assertThrows(
        IllegalStateException.class,
        () ->
            Determinizer.determinize(
                nfa,
                Determinizer.byEquality(),
                outputs -> {
                  if (outputs.size() > 1) throw new IllegalStateException("ambiguous: " + outputs);
                  return outputs.iterator().next();
                }));
  }

  @Test
  void outputResolver_customPriority_picksExpected() {
    // Custom resolver always picks "HIGH" when present in the candidates.
    var nfa = Automaton.<String, Character>create();
    var root = nfa.newState();
    nfa.setInitialStateId(root.getId());
    root.addTransition('x', nfa.newState("HIGH"));
    root.addTransition('x', nfa.newState("LOW"));

    var dfa =
        Determinizer.determinize(
            nfa,
            Determinizer.byEquality(),
            outputs -> outputs.contains("HIGH") ? "HIGH" : outputs.iterator().next());

    var next = dfa.getInitial().getTransitions().get(0).target();
    assertEquals("HIGH", next.getOutput());
  }
}
