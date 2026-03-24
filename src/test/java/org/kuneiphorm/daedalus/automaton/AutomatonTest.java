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
