package org.kuneiphorm.daedalus.automaton;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AutomatonTest {

  @Test
  void create_startsEmpty() {
    var a = Automaton.<String, Integer>create();
    assertTrue(a.getStates().isEmpty());
  }

  @Test
  void getInitial_throwsWhenNotSet() {
    var a = Automaton.<String, Integer>create();
    assertThrows(IllegalStateException.class, a::getInitial);
  }

  @Test
  void setInitialStateId_invalidId_throws() {
    var a = Automaton.<String, Integer>create();
    assertThrows(IllegalArgumentException.class, () -> a.setInitialStateId(0));
  }

  @Test
  void setInitialStateId_negativeId_throws() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    assertThrows(IllegalArgumentException.class, () -> a.setInitialStateId(-1));
  }

  @Test
  void setInitialStateId_validId_getInitialReturnsCorrectState() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    a.setInitialStateId(1);
    assertSame(q1, a.getInitial());
    assertNotSame(q0, a.getInitial());
  }

  @Test
  void newState_assignsSequentialIds() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    var q2 = a.newState();
    assertEquals(0, q0.getId());
    assertEquals(1, q1.getId());
    assertEquals(2, q2.getId());
  }

  @Test
  void newState_withOutput_isAccepting() {
    var a = Automaton.<String, Integer>create();
    var q = a.newState("TOKEN");
    assertTrue(q.isAccepting());
    assertEquals("TOKEN", q.getOutput());
  }

  @Test
  void newState_noOutput_isNotAccepting() {
    var a = Automaton.<String, Integer>create();
    var q = a.newState();
    assertFalse(q.isAccepting());
    assertNull(q.getOutput());
  }

  @Test
  void getStates_isUnmodifiable() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    assertThrows(UnsupportedOperationException.class, () -> a.getStates().clear());
  }

  // --- isDeterministic ---

  @Test
  void isDeterministic_noTransitions_returnsTrue() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    assertTrue(a.isDeterministic());
  }

  @Test
  void isDeterministic_distinctLabels_returnsTrue() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    q0.addTransition(1, q1);
    q0.addTransition(2, q1);
    assertTrue(a.isDeterministic());
  }

  @Test
  void isDeterministic_epsilonTransition_returnsFalse() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    q0.addEpsilonTransition(q1);
    assertFalse(a.isDeterministic());
  }

  @Test
  void isDeterministic_duplicateLabel_returnsFalse() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    var q2 = a.newState();
    q0.addTransition(1, q1);
    q0.addTransition(1, q2);
    assertFalse(a.isDeterministic());
  }

  @Test
  void isDeterministic_empty_returnsTrue() {
    var a = Automaton.<String, Integer>create();
    assertTrue(a.isDeterministic());
  }

  // --- stateCount ---

  @Test
  void stateCount_empty_returnsZero() {
    var a = Automaton.<String, Integer>create();
    assertEquals(0, a.stateCount());
  }

  @Test
  void stateCount_afterCreation_returnsCount() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    a.newState();
    a.newState();
    assertEquals(3, a.stateCount());
  }

  // --- getAcceptingStates ---

  @Test
  void getAcceptingStates_noAccepting_returnsEmpty() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    a.newState();
    assertTrue(a.getAcceptingStates().isEmpty());
  }

  @Test
  void getAcceptingStates_returnsOnlyAccepting() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    var q1 = a.newState("A");
    a.newState();
    var q3 = a.newState("B");
    var accepting = a.getAcceptingStates();
    assertEquals(2, accepting.size());
    assertSame(q1, accepting.get(0));
    assertSame(q3, accepting.get(1));
  }

  @Test
  void getAcceptingStates_isUnmodifiable() {
    var a = Automaton.<String, Integer>create();
    a.newState("TOKEN");
    assertThrows(UnsupportedOperationException.class, () -> a.getAcceptingStates().clear());
  }

  // --- toString ---

  @Test
  void toString_noStates() {
    var a = Automaton.<String, Integer>create();
    assertEquals("Automaton[0 states]", a.toString());
  }

  @Test
  void toString_singleState_singularWord() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    a.setInitialStateId(0);
    assertTrue(a.toString().contains("Automaton[1 state]"));
    assertFalse(a.toString().contains("states"));
  }

  @Test
  void toString_marksInitialState() {
    var a = Automaton.<String, Integer>create();
    a.newState();
    a.newState();
    a.setInitialStateId(1);
    var s = a.toString();
    assertTrue(s.contains("q1 (initial)"));
    assertFalse(s.contains("q0 (initial)"));
  }

  @Test
  void toString_includesTransitions() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState("T");
    q0.addTransition(42, q1);
    a.setInitialStateId(0);
    var s = a.toString();
    assertTrue(s.contains("-42-> q1[T]"));
  }
}
