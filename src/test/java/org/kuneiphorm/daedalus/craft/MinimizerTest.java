package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.core.Expression;

class MinimizerTest {

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

  private static <S, L> Automaton<S, L> buildMinDfa(Expression<L> expr, S output) {
    return Minimizer.minimize(Determinizer.determinize(ExpressionConverter.build(expr, output)));
  }

  // --- State count reduction ---

  @Test
  void redundantBranches_stateCountReduced() {
    // (a|b)c produces a DFA where the states after 'a' and after 'b' are equivalent
    // (both have only c->accept). Minimizer should merge them: 4 states -> 3.
    var expr =
        Expression.sequence(
            Expression.choice(Expression.unit('a'), Expression.unit('b')), Expression.unit('c'));
    var dfa = Determinizer.determinize(ExpressionConverter.build(expr, "TOKEN"));
    var minDfa = Minimizer.minimize(dfa);
    assertTrue(minDfa.getStates().size() < dfa.getStates().size());
    assertEquals(3, minDfa.getStates().size());
  }

  // --- Acceptance preserved ---

  @Test
  void unit_acceptsMatchingChar() {
    var min = buildMinDfa(Expression.unit('a'), "TOKEN");
    assertTrue(dfaAccepts(min, List.of('a')));
    assertFalse(dfaAccepts(min, List.of('b')));
    assertFalse(dfaAccepts(min, List.of()));
  }

  @Test
  void sequence_acceptancePreserved() {
    var min = buildMinDfa(Expression.sequence(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertTrue(dfaAccepts(min, List.of('a', 'b')));
    assertFalse(dfaAccepts(min, List.of('a')));
    assertFalse(dfaAccepts(min, List.of('b', 'a')));
  }

  @Test
  void choice_acceptancePreserved() {
    var min = buildMinDfa(Expression.choice(Expression.unit('a'), Expression.unit('b')), "TOKEN");
    assertTrue(dfaAccepts(min, List.of('a')));
    assertTrue(dfaAccepts(min, List.of('b')));
    assertFalse(dfaAccepts(min, List.of('c')));
  }

  @Test
  void star_acceptancePreserved() {
    var min = buildMinDfa(Expression.star(Expression.unit('a')), "TOKEN");
    assertTrue(dfaAccepts(min, List.of()));
    assertTrue(dfaAccepts(min, List.of('a')));
    assertTrue(dfaAccepts(min, List.of('a', 'a', 'a')));
    assertFalse(dfaAccepts(min, List.of('b')));
  }

  @Test
  void plus_acceptancePreserved() {
    var min = buildMinDfa(Expression.plus(Expression.unit('a')), "TOKEN");
    assertFalse(dfaAccepts(min, List.of()));
    assertTrue(dfaAccepts(min, List.of('a')));
    assertTrue(dfaAccepts(min, List.of('a', 'a')));
  }

  // --- State count never increases ---

  @Test
  void stateCountNeverIncreases() {
    var expr = Expression.choice(Expression.unit('a'), Expression.unit('b'));
    var dfa = Determinizer.determinize(ExpressionConverter.build(expr, "TOKEN"));
    var minDfa = Minimizer.minimize(dfa);
    assertTrue(minDfa.getStates().size() <= dfa.getStates().size());
  }
}
