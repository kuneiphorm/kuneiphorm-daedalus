package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.core.Expression;

class TrimmerTest {

  // Build a DFA from an expression for acceptance tests.
  private static Automaton<String, Integer> buildDfa(Expression<Integer> expr) {
    return Determinizer.determinize(ExpressionConverter.build(expr, "TOKEN"));
  }

  // Simulate DFA acceptance.
  private static <S> boolean accepts(Automaton<S, Integer> dfa, String input) {
    var current = dfa.getInitial();
    for (char ch : input.toCharArray()) {
      var label = (int) ch;
      var next =
          current.getTransitions().stream()
              .filter(t -> t.label() != null && t.label().equals(label))
              .map(t -> t.target())
              .findFirst()
              .orElse(null);
      if (next == null) return false;
      current = next;
    }
    return current.isAccepting();
  }

  // --- Null checks ---

  @Test
  void trim_nullDfa_throwsNpe() {
    assertThrows(NullPointerException.class, () -> Trimmer.trim(null));
  }

  // --- Non-deterministic rejection ---

  @Test
  void trim_epsilonTransitions_throwsIae() {
    Automaton<String, Integer> nfa = Automaton.create();
    State<String, Integer> q0 = nfa.newState();
    State<String, Integer> q1 = nfa.newState("TOKEN");
    q0.addEpsilonTransition(q1);
    nfa.setInitialStateId(q0.getId());
    assertThrows(IllegalArgumentException.class, () -> Trimmer.trim(nfa));
  }

  @Test
  void trim_duplicateLabels_throwsIae() {
    Automaton<String, Integer> nfa = Automaton.create();
    State<String, Integer> q0 = nfa.newState();
    State<String, Integer> q1 = nfa.newState("A");
    State<String, Integer> q2 = nfa.newState("B");
    q0.addTransition(97, q1);
    q0.addTransition(97, q2);
    nfa.setInitialStateId(q0.getId());
    assertThrows(IllegalArgumentException.class, () -> Trimmer.trim(nfa));
  }

  // --- Reachability ---

  @Test
  void unreachableState_isRemoved() {
    var dfa = Automaton.<String, Integer>create();
    var q0 = dfa.newState();
    var q1 = dfa.newState("TOKEN");
    dfa.newState("TOKEN"); // unreachable: no incoming transitions from q0
    q0.addTransition((int) 'a', q1);
    dfa.setInitialStateId(q0.getId());

    var trimmed = Trimmer.trim(dfa);
    assertEquals(2, trimmed.getStates().size());
  }

  @Test
  void allReachable_stateCountUnchanged() {
    var dfa = buildDfa(Expression.unit((int) 'a'));
    var trimmed = Trimmer.trim(dfa);
    // Determinizer output is always fully reachable
    assertEquals(dfa.getStates().size(), trimmed.getStates().size());
  }

  // --- Co-reachability (useless states) ---

  @Test
  void deadState_isRemoved() {
    // q0 -a-> q1(accept), q0 -b-> q2(dead, non-accepting, no outgoing)
    var dfa = Automaton.<String, Integer>create();
    var q0 = dfa.newState();
    var q1 = dfa.newState("TOKEN");
    var q2 = dfa.newState(); // dead: reachable but cannot reach any accepting state
    q0.addTransition((int) 'a', q1);
    q0.addTransition((int) 'b', q2);
    dfa.setInitialStateId(q0.getId());

    var trimmed = Trimmer.trim(dfa);
    assertEquals(2, trimmed.getStates().size()); // q0 and q1 only
  }

  @Test
  void deadStateTransition_isDropped() {
    var dfa = Automaton.<String, Integer>create();
    var q0 = dfa.newState();
    var q1 = dfa.newState("TOKEN");
    var q2 = dfa.newState();
    q0.addTransition((int) 'a', q1);
    q0.addTransition((int) 'b', q2);
    dfa.setInitialStateId(q0.getId());

    var trimmed = Trimmer.trim(dfa);
    // q0 in trimmed should only have the transition to q1, not to q2
    assertEquals(1, trimmed.getInitial().getTransitions().size());
    assertEquals((int) 'a', trimmed.getInitial().getTransitions().get(0).label());
  }

  // --- Acceptance preserved ---

  @Test
  void unit_acceptancePreserved() {
    var dfa = buildDfa(Expression.unit((int) 'a'));
    var trimmed = Trimmer.trim(dfa);
    assertTrue(accepts(trimmed, "a"));
    assertFalse(accepts(trimmed, "b"));
    assertFalse(accepts(trimmed, ""));
  }

  @Test
  void sequence_acceptancePreserved() {
    var dfa = buildDfa(Expression.sequence(Expression.unit((int) 'a'), Expression.unit((int) 'b')));
    var trimmed = Trimmer.trim(dfa);
    assertTrue(accepts(trimmed, "ab"));
    assertFalse(accepts(trimmed, "a"));
    assertFalse(accepts(trimmed, "ba"));
  }

  @Test
  void star_acceptancePreserved() {
    var dfa = buildDfa(Expression.star(Expression.unit((int) 'a')));
    var trimmed = Trimmer.trim(dfa);
    assertTrue(accepts(trimmed, ""));
    assertTrue(accepts(trimmed, "a"));
    assertTrue(accepts(trimmed, "aaa"));
    assertFalse(accepts(trimmed, "b"));
  }

  // --- Edge cases ---

  @Test
  void alreadyTrim_stateCountUnchanged() {
    var dfa = buildDfa(Expression.choice(Expression.unit((int) 'a'), Expression.unit((int) 'b')));
    var minDfa = Minimizer.minimize(dfa);
    var trimmed = Trimmer.trim(minDfa);
    assertEquals(minDfa.getStates().size(), trimmed.getStates().size());
  }

  @Test
  void initialStateAlwaysKept_evenIfNotCoReachable() {
    // DFA that accepts nothing: q0 non-accepting, no accepting states at all
    var dfa = Automaton.<String, Integer>create();
    var q0 = dfa.newState();
    var q1 = dfa.newState();
    q0.addTransition((int) 'a', q1);
    dfa.setInitialStateId(q0.getId());

    var trimmed = Trimmer.trim(dfa);
    // Initial state is always preserved even when language is empty
    assertNotNull(trimmed.getInitial());
    assertFalse(trimmed.getInitial().isAccepting());
  }
}
