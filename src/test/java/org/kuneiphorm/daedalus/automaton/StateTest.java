package org.kuneiphorm.daedalus.automaton;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StateTest {

  private static Automaton<String, Integer> automaton() {
    return Automaton.create();
  }

  @Test
  void newState_getId_returnsId() {
    var a = automaton();
    var q = a.newState();
    assertEquals(0, q.getId());
  }

  @Test
  void newState_isNotAccepting() {
    var a = automaton();
    var q = a.newState();
    assertFalse(q.isAccepting());
    assertNull(q.getOutput());
  }

  @Test
  void newStateWithOutput_isAccepting() {
    var a = automaton();
    var q = a.newState("TOKEN");
    assertTrue(q.isAccepting());
    assertEquals("TOKEN", q.getOutput());
  }

  @Test
  void setOutput_makesStateAccepting() {
    var a = automaton();
    var q = a.newState();
    assertFalse(q.isAccepting());
    q.setOutput("X");
    assertTrue(q.isAccepting());
    assertEquals("X", q.getOutput());
  }

  @Test
  void clearOutput_makesStateNonAccepting() {
    var a = automaton();
    var q = a.newState("X");
    assertTrue(q.isAccepting());
    q.clearOutput();
    assertFalse(q.isAccepting());
    assertNull(q.getOutput());
  }

  @Test
  void addTransition_appearsInTransitions() {
    var a = automaton();
    var q0 = a.newState();
    var q1 = a.newState();
    q0.addTransition(1, q1);
    assertEquals(1, q0.getTransitions().size());
    var t = q0.getTransitions().get(0);
    assertEquals(1, t.label());
    assertSame(q1, t.target());
    assertFalse(t.isEpsilon());
  }

  @Test
  void addEpsilonTransition_appearsAsEpsilon() {
    var a = automaton();
    var q0 = a.newState();
    var q1 = a.newState();
    q0.addEpsilonTransition(q1);
    assertEquals(1, q0.getTransitions().size());
    var t = q0.getTransitions().get(0);
    assertNull(t.label());
    assertTrue(t.isEpsilon());
    assertSame(q1, t.target());
  }

  @Test
  void getTransitions_isUnmodifiable() {
    var a = automaton();
    var q = a.newState();
    assertThrows(UnsupportedOperationException.class, () -> q.getTransitions().clear());
  }

  @Test
  void toString_nonAccepting() {
    var a = automaton();
    var q = a.newState();
    assertEquals("q0", q.toString());
  }

  @Test
  void toString_accepting() {
    var a = automaton();
    var q = a.newState("OUT");
    assertEquals("q0[OUT]", q.toString());
  }
}
