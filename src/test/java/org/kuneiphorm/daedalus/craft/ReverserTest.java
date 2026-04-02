package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;

/**
 * Tests for {@link Reverser}.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
class ReverserTest {

  // --- Null checks ---

  @Test
  void reverse_nullAutomaton_throwsNpe() {
    assertThrows(NullPointerException.class, () -> Reverser.reverse(null, null, "X"));
  }

  @Test
  void reverse_nullStartState_throwsNpe() {
    Automaton<String, Integer> a = Automaton.create();
    a.newState();
    a.setInitialStateId(0);
    assertThrows(NullPointerException.class, () -> Reverser.reverse(a, null, "X"));
  }

  @Test
  void reverse_nullOutput_throwsNpe() {
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    a.setInitialStateId(0);
    assertThrows(NullPointerException.class, () -> Reverser.reverse(a, q0, null));
  }

  // --- Simple chain: q0 --a--> q1 --b--> q2[OK] ---

  @Test
  void reverse_simpleChain_flipsEdges() {
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    State<String, Integer> q2 = a.newState("OK");
    q0.addTransition(97, q1);
    q1.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> rev = Reverser.reverse(a, q2, "REV");

    assertEquals(3, rev.stateCount());
    assertEquals(q2.getId(), rev.getInitial().getId());

    // q0 in reversed automaton should be accepting with "REV"
    assertTrue(rev.getStates().get(q0.getId()).isAccepting());
    assertEquals("REV", rev.getStates().get(q0.getId()).getOutput());

    // q1, q2 should not be accepting
    assertFalse(rev.getStates().get(q1.getId()).isAccepting());
    assertFalse(rev.getStates().get(q2.getId()).isAccepting());

    // Reversed edges: q2 --98--> q1 --97--> q0
    assertEquals(1, rev.getStates().get(q2.getId()).getTransitions().size());
    assertEquals(
        q1.getId(), rev.getStates().get(q2.getId()).getTransitions().getFirst().target().getId());
    assertEquals(98, rev.getStates().get(q2.getId()).getTransitions().getFirst().label());

    assertEquals(1, rev.getStates().get(q1.getId()).getTransitions().size());
    assertEquals(
        q0.getId(), rev.getStates().get(q1.getId()).getTransitions().getFirst().target().getId());
    assertEquals(97, rev.getStates().get(q1.getId()).getTransitions().getFirst().label());
  }

  @Test
  void reverse_simpleChain_acceptsReversedString() {
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    State<String, Integer> q2 = a.newState("OK");
    q0.addTransition(97, q1);
    q1.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> rev = Reverser.reverse(a, q2, "REV");

    assertTrue(accepts(rev, "ba"));
    assertFalse(accepts(rev, "ab"));
    assertFalse(accepts(rev, "a"));
    assertFalse(accepts(rev, ""));
  }

  // --- Branching: q0 --a--> q1[X], q0 --b--> q2[Y] ---

  @Test
  void reverse_branching_fromOneAccepting() {
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState("X");
    State<String, Integer> q2 = a.newState("Y");
    q0.addTransition(97, q1);
    q0.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> rev = Reverser.reverse(a, q1, "REV");

    assertEquals(q1.getId(), rev.getInitial().getId());
    assertTrue(rev.getStates().get(q0.getId()).isAccepting());
    assertFalse(rev.getStates().get(q1.getId()).isAccepting());
    assertFalse(rev.getStates().get(q2.getId()).isAccepting());

    assertTrue(accepts(rev, "a"));
    assertFalse(accepts(rev, "b"));
  }

  // --- Epsilon transitions ---

  @Test
  void reverse_epsilonTransitions_areFlipped() {
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState("OK");
    q0.addEpsilonTransition(q1);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> rev = Reverser.reverse(a, q1, "REV");

    assertEquals(1, rev.getStates().get(q1.getId()).getTransitions().size());
    assertTrue(rev.getStates().get(q1.getId()).getTransitions().getFirst().isEpsilon());
    assertEquals(
        q0.getId(), rev.getStates().get(q1.getId()).getTransitions().getFirst().target().getId());
  }

  // --- Self-loop ---

  @Test
  void reverse_selfLoop_preserved() {
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState("OK");
    q0.addTransition(97, q0);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> rev = Reverser.reverse(a, q0, "REV");

    assertEquals(1, rev.stateCount());
    assertTrue(rev.getStates().get(0).isAccepting());
    assertEquals("REV", rev.getStates().get(0).getOutput());
    assertEquals(1, rev.getStates().get(0).getTransitions().size());
    assertEquals(0, rev.getStates().get(0).getTransitions().getFirst().target().getId());
  }

  // --- Reverse from initial state itself ---

  @Test
  void reverse_fromInitial_initialBecomesAccepting() {
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState("OK");
    q0.addTransition(97, q1);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> rev = Reverser.reverse(a, q0, "REV");

    assertEquals(q0.getId(), rev.getInitial().getId());
    assertTrue(rev.getStates().get(q0.getId()).isAccepting());
    assertEquals("REV", rev.getStates().get(q0.getId()).getOutput());
  }

  // --- Different output type ---

  @Test
  void reverse_differentOutputType() {
    Automaton<Boolean, Integer> a = Automaton.create();
    State<Boolean, Integer> q0 = a.newState();
    State<Boolean, Integer> q1 = a.newState(Boolean.TRUE);
    q0.addTransition(97, q1);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> rev = Reverser.reverse(a, q1, "REVERSED");

    assertEquals(q1.getId(), rev.getInitial().getId());
    assertTrue(rev.getStates().get(q0.getId()).isAccepting());
    assertEquals("REVERSED", rev.getStates().get(q0.getId()).getOutput());
    assertFalse(rev.getStates().get(q1.getId()).isAccepting());
  }

  // --- Helper ---

  private static <S> boolean accepts(Automaton<S, Integer> dfa, String input) {
    State<S, Integer> current = dfa.getInitial();
    for (char ch : input.toCharArray()) {
      int label = ch;
      State<S, Integer> next = null;
      for (var t : current.getTransitions()) {
        if (t.label() != null && t.label().equals(label)) {
          next = t.target();
          break;
        }
      }
      if (next == null) {
        return false;
      }
      current = next;
    }
    return current.isAccepting();
  }
}
