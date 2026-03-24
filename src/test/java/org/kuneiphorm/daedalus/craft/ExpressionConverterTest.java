package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.core.Expression;

class ExpressionConverterTest {

  // --- NFA simulation helpers ---

  private static <S, L> Set<State<S, L>> epsilonClosure(Set<State<S, L>> states) {
    var closure = new HashSet<>(states);
    var worklist = new ArrayDeque<>(states);
    while (!worklist.isEmpty()) {
      var state = worklist.poll();
      for (var t : state.getTransitions()) {
        if (t.isEpsilon() && closure.add(t.target())) {
          worklist.add(t.target());
        }
      }
    }
    return closure;
  }

  private static <S, L> boolean nfaAccepts(Automaton<S, L> nfa, List<L> input) {
    var current = epsilonClosure(Set.of(nfa.getInitial()));
    for (var symbol : input) {
      var next = new HashSet<State<S, L>>();
      for (var state : current) {
        for (var t : state.getTransitions()) {
          if (!t.isEpsilon() && symbol.equals(t.label())) {
            next.add(t.target());
          }
        }
      }
      current = epsilonClosure(next);
    }
    return current.stream().anyMatch(State::isAccepting);
  }

  // --- Null checks ---

  @Test
  void build_nullExpression_throwsNpe() {
    assertThrows(NullPointerException.class, () -> ExpressionConverter.build(null, "TOKEN"));
  }

  // --- Unit ---

  @Test
  void unit_initialStateIsSet() {
    var nfa = ExpressionConverter.build(Expression.unit('a'), "TOKEN");
    assertDoesNotThrow(nfa::getInitial);
  }

  @Test
  void unit_hasExactlyOneAcceptingState() {
    var nfa = ExpressionConverter.build(Expression.unit('a'), "TOKEN");
    var accepting = nfa.getStates().stream().filter(State::isAccepting).toList();
    assertEquals(1, accepting.size());
    assertEquals("TOKEN", accepting.get(0).getOutput());
  }

  @Test
  void unit_acceptsMatchingChar() {
    var nfa = ExpressionConverter.build(Expression.unit('a'), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a')));
  }

  @Test
  void unit_rejectsOtherChar() {
    var nfa = ExpressionConverter.build(Expression.unit('a'), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of('b')));
  }

  @Test
  void unit_rejectsEpsilon() {
    var nfa = ExpressionConverter.build(Expression.unit('a'), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of()));
  }

  // --- Sequence ---

  @Test
  void sequence_acceptsMatchingPair() {
    var nfa =
        ExpressionConverter.build(
            Expression.sequence(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a', 'b')));
  }

  @Test
  void sequence_rejectsPartial() {
    var nfa =
        ExpressionConverter.build(
            Expression.sequence(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of('a')));
    assertFalse(nfaAccepts(nfa, List.of('b')));
  }

  @Test
  void sequence_rejectsWrongOrder() {
    var nfa =
        ExpressionConverter.build(
            Expression.sequence(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of('b', 'a')));
  }

  @Test
  void emptySequence_acceptsEpsilon() {
    var nfa = ExpressionConverter.build(Expression.sequence(), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of()));
  }

  @Test
  void emptySequence_rejectsAnything() {
    var nfa = ExpressionConverter.build(Expression.sequence(), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of('a')));
  }

  // --- Choice ---

  @Test
  void choice_acceptsFirstAlternative() {
    var nfa =
        ExpressionConverter.build(
            Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a')));
  }

  @Test
  void choice_acceptsSecondAlternative() {
    var nfa =
        ExpressionConverter.build(
            Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('b')));
  }

  @Test
  void choice_rejectsOther() {
    var nfa =
        ExpressionConverter.build(
            Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of('c')));
  }

  @Test
  void emptyChoice_rejectsEverything() {
    var nfa = ExpressionConverter.build(Expression.choice(), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of()));
    assertFalse(nfaAccepts(nfa, List.of('a')));
  }

  // --- Optional ---

  @Test
  void optional_acceptsEpsilon() {
    var nfa = ExpressionConverter.build(Expression.optional(Expression.unit('a')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of()));
  }

  @Test
  void optional_acceptsOne() {
    var nfa = ExpressionConverter.build(Expression.optional(Expression.unit('a')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a')));
  }

  @Test
  void optional_rejectsMore() {
    var nfa = ExpressionConverter.build(Expression.optional(Expression.unit('a')), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of('a', 'a')));
  }

  // --- Star ---

  @Test
  void star_acceptsEpsilon() {
    var nfa = ExpressionConverter.build(Expression.star(Expression.unit('a')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of()));
  }

  @Test
  void star_acceptsOne() {
    var nfa = ExpressionConverter.build(Expression.star(Expression.unit('a')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a')));
  }

  @Test
  void star_acceptsMany() {
    var nfa = ExpressionConverter.build(Expression.star(Expression.unit('a')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a', 'a', 'a')));
  }

  @Test
  void star_rejectsWrongChar() {
    var nfa = ExpressionConverter.build(Expression.star(Expression.unit('a')), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of('b')));
  }

  // --- Plus ---

  @Test
  void plus_rejectsEpsilon() {
    var nfa = ExpressionConverter.build(Expression.plus(Expression.unit('a')), "TOKEN");
    assertFalse(nfaAccepts(nfa, List.of()));
  }

  @Test
  void plus_acceptsOne() {
    var nfa = ExpressionConverter.build(Expression.plus(Expression.unit('a')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a')));
  }

  @Test
  void plus_acceptsMany() {
    var nfa = ExpressionConverter.build(Expression.plus(Expression.unit('a')), "TOKEN");
    assertTrue(nfaAccepts(nfa, List.of('a', 'a', 'a')));
  }
}
