package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;

/**
 * Tests for {@link OutputRefiner}.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
class OutputRefinerTest {

  // --- computeOutputReachability ---

  @Test
  void reachability_singleAcceptingState() {
    // q0 --a--> q1[X]
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState("X");
    q0.addTransition(97, q1);
    a.setInitialStateId(q0.getId());

    List<Set<String>> reachable = OutputRefiner.computeOutputReachability(a);
    assertEquals(Set.of("X"), reachable.get(q0.getId()));
    assertEquals(Set.of("X"), reachable.get(q1.getId()));
  }

  @Test
  void reachability_twoDistinctOutputs() {
    // q0 --a--> q1[X], q0 --b--> q2[Y]
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState("X");
    State<String, Integer> q2 = a.newState("Y");
    q0.addTransition(97, q1);
    q0.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    List<Set<String>> reachable = OutputRefiner.computeOutputReachability(a);
    assertEquals(Set.of("X", "Y"), reachable.get(q0.getId()));
    assertEquals(Set.of("X"), reachable.get(q1.getId()));
    assertEquals(Set.of("Y"), reachable.get(q2.getId()));
  }

  @Test
  void reachability_chain() {
    // q0 --a--> q1 --b--> q2[X]
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    State<String, Integer> q2 = a.newState("X");
    q0.addTransition(97, q1);
    q1.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    List<Set<String>> reachable = OutputRefiner.computeOutputReachability(a);
    assertEquals(Set.of("X"), reachable.get(q0.getId()));
    assertEquals(Set.of("X"), reachable.get(q1.getId()));
    assertEquals(Set.of("X"), reachable.get(q2.getId()));
  }

  @Test
  void reachability_deadState() {
    // q0 --a--> q1 (no accepting state reachable from q1)
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    q0.addTransition(97, q1);
    a.setInitialStateId(q0.getId());

    List<Set<String>> reachable = OutputRefiner.computeOutputReachability(a);
    assertEquals(Set.of(), reachable.get(q0.getId()));
    assertEquals(Set.of(), reachable.get(q1.getId()));
  }

  @Test
  void reachability_selfLoop() {
    // q0[X] --a--> q0
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState("X");
    q0.addTransition(97, q0);
    a.setInitialStateId(q0.getId());

    List<Set<String>> reachable = OutputRefiner.computeOutputReachability(a);
    assertEquals(Set.of("X"), reachable.get(q0.getId()));
  }

  @Test
  void reachability_diamondWithSharedOutput() {
    // q0 --a--> q1 --c--> q3[X]
    // q0 --b--> q2 --d--> q3[X]
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    State<String, Integer> q2 = a.newState();
    State<String, Integer> q3 = a.newState("X");
    q0.addTransition(97, q1);
    q0.addTransition(98, q2);
    q1.addTransition(99, q3);
    q2.addTransition(100, q3);
    a.setInitialStateId(q0.getId());

    List<Set<String>> reachable = OutputRefiner.computeOutputReachability(a);
    assertEquals(Set.of("X"), reachable.get(q0.getId()));
    assertEquals(Set.of("X"), reachable.get(q1.getId()));
    assertEquals(Set.of("X"), reachable.get(q2.getId()));
  }

  // --- refine ---

  @Test
  void refine_singletonPath_marksEarlyAccept() {
    // q0 --a--> q1 --b--> q2[X]
    // After refinement: q0 is decided (only reaches X), so it becomes accepting and
    // q1, q2 are trimmed.
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    State<String, Integer> q2 = a.newState("X");
    q0.addTransition(97, q1);
    q1.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> refined = OutputRefiner.refine(a);

    // Only the initial state survives -- it accepts immediately with "X".
    assertEquals(1, refined.stateCount());
    assertTrue(refined.getInitial().isAccepting());
    assertEquals("X", refined.getInitial().getOutput());
    assertTrue(refined.getInitial().getTransitions().isEmpty());
  }

  @Test
  void refine_branchingKeepsUndecidedStates() {
    // q0 --a--> q1[X], q0 --b--> q2[Y]
    // q0 is undecided (reaches both X and Y), q1 and q2 are decided.
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState("X");
    State<String, Integer> q2 = a.newState("Y");
    q0.addTransition(97, q1);
    q0.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> refined = OutputRefiner.refine(a);

    // q0 still undecided, q1 and q2 are accepting leaves.
    assertEquals(3, refined.stateCount());
    assertFalse(refined.getInitial().isAccepting());
    assertEquals(2, refined.getInitial().getTransitions().size());
  }

  @Test
  void refine_decisionDfaPattern() {
    // Mimics the LR-Regular decision DFA: q0 --c--> q0, q0 --d--> q1[A], q0 --e--> q2[B]
    // q0 is undecided (reaches A and B). q1 and q2 are decided.
    // No states should be trimmed.
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState("A");
    State<String, Integer> q2 = a.newState("B");
    q0.addTransition(99, q0); // 'c' self-loop
    q0.addTransition(100, q1); // 'd' -> A
    q0.addTransition(101, q2); // 'e' -> B
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> refined = OutputRefiner.refine(a);

    // All 3 states survive. q0 still undecided.
    assertEquals(3, refined.stateCount());
    assertFalse(refined.getInitial().isAccepting());
    // q0 still has the self-loop and two outgoing transitions
    assertEquals(3, refined.getInitial().getTransitions().size());
  }

  @Test
  void refine_trimsStaleAfterDecision() {
    // q0 --a--> q1 --b--> q2[X], q0 --c--> q3[Y]
    // q1 is decided (only reaches X) -- its outgoing transitions are dropped.
    // q2 becomes unreachable and is trimmed.
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    State<String, Integer> q2 = a.newState("X");
    State<String, Integer> q3 = a.newState("Y");
    q0.addTransition(97, q1);
    q1.addTransition(98, q2);
    q0.addTransition(99, q3);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> refined = OutputRefiner.refine(a);

    // q2 is trimmed. q0 (undecided), q1 (decided X), q3 (decided Y) survive.
    assertEquals(3, refined.stateCount());
    assertFalse(refined.getInitial().isAccepting());
  }

  @Test
  void refine_preservesOutputOnUndecidedState() {
    // q0[Z] --a--> q1[X], q0 --b--> q2[Y]
    // q0 has output Z but reaches X and Y -- undecided, keeps Z.
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState("Z");
    State<String, Integer> q1 = a.newState("X");
    State<String, Integer> q2 = a.newState("Y");
    q0.addTransition(97, q1);
    q0.addTransition(98, q2);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> refined = OutputRefiner.refine(a);

    // q0 keeps its original output "Z" even though it's undecided.
    assertTrue(refined.getInitial().isAccepting());
    assertEquals("Z", refined.getInitial().getOutput());
  }

  @Test
  void refine_allDead_onlyInitialSurvives() {
    // q0 --a--> q1 (no accepting states)
    Automaton<String, Integer> a = Automaton.create();
    State<String, Integer> q0 = a.newState();
    State<String, Integer> q1 = a.newState();
    q0.addTransition(97, q1);
    a.setInitialStateId(q0.getId());

    Automaton<String, Integer> refined = OutputRefiner.refine(a);

    // Trimmer keeps the initial state even when not co-reachable.
    assertEquals(1, refined.stateCount());
    assertNull(refined.getInitial().getOutput());
  }

  // --- Validation ---

  @Test
  void refine_nfa_throwsIae() {
    Automaton<String, Integer> nfa = Automaton.create();
    State<String, Integer> q0 = nfa.newState();
    State<String, Integer> q1 = nfa.newState("X");
    q0.addEpsilonTransition(q1);
    nfa.setInitialStateId(q0.getId());
    assertThrows(IllegalArgumentException.class, () -> OutputRefiner.refine(nfa));
  }

  @Test
  void refine_nullAutomaton_throwsNpe() {
    assertThrows(NullPointerException.class, () -> OutputRefiner.refine(null));
  }

  @Test
  void computeOutputReachability_nullAutomaton_throwsNpe() {
    assertThrows(NullPointerException.class, () -> OutputRefiner.computeOutputReachability(null));
  }
}
