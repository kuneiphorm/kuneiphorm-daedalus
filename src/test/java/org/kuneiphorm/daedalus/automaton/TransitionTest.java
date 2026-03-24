package org.kuneiphorm.daedalus.automaton;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TransitionTest {

  @Test
  void labeledTransition_isNotEpsilon() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    q0.addTransition(5, q1);
    var t = q0.getTransitions().get(0);
    assertFalse(t.isEpsilon());
    assertEquals(5, t.label());
    assertSame(q1, t.target());
  }

  @Test
  void epsilonTransition_isEpsilon() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    q0.addEpsilonTransition(q1);
    var t = q0.getTransitions().get(0);
    assertTrue(t.isEpsilon());
    assertNull(t.label());
    assertSame(q1, t.target());
  }

  @Test
  void toString_labeled() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState("T");
    q0.addTransition(7, q1);
    var t = q0.getTransitions().get(0);
    assertEquals("-7-> q1[T]", t.toString());
  }

  @Test
  void toString_epsilon() {
    var a = Automaton.<String, Integer>create();
    var q0 = a.newState();
    var q1 = a.newState();
    q0.addEpsilonTransition(q1);
    var t = q0.getTransitions().get(0);
    assertEquals("-ε-> q1", t.toString());
  }
}
