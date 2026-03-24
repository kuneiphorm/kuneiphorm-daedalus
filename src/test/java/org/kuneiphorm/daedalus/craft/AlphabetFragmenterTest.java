package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.automaton.Transition;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.daedalus.range.FragmentedAutomaton;
import org.kuneiphorm.daedalus.range.IntRange;

class AlphabetFragmenterTest {

  // Build a fragmented DFA from an IntRange expression via RangeDeterminizer pipeline.
  private static FragmentedAutomaton<String> buildFragmented(Expression<IntRange> expr) {
    Automaton<String, IntRange> nfa = ExpressionConverter.build(expr, "TOKEN");
    Automaton<String, IntRange> dfa = RangeDeterminizer.determinize(nfa, Map.of());
    Automaton<String, IntRange> minDfa = Minimizer.minimize(dfa);
    return AlphabetFragmenter.fragment(minDfa);
  }

  // Accept a list of integer inputs through a FragmentedAutomaton by classifying each value.
  private static <S> boolean fragmentedAccepts(FragmentedAutomaton<S> fa, List<Integer> input) {
    State<S, Integer> current = fa.dfa().getInitial();
    for (int ch : input) {
      int fragmentId = fa.classifier().classify(ch);
      if (fragmentId == -1) return false;
      State<S, Integer> next = null;
      for (Transition<S, Integer> t : current.getTransitions()) {
        if (t.label().equals(fragmentId)) {
          next = t.target();
          break;
        }
      }
      if (next == null) return false;
      current = next;
    }
    return current.isAccepting();
  }

  // --- Null checks ---

  @Test
  void fragment_nullDfa_throwsNpe() {
    assertThrows(NullPointerException.class, () -> AlphabetFragmenter.fragment(null));
  }

  // --- Classifier correctness ---

  @Test
  void classifier_mapsLabelToValidFragmentId() {
    FragmentedAutomaton<String> fa = buildFragmented(Expression.unit(new IntRange(97, 97)));
    int fragmentId = fa.classifier().classify(97);
    assertTrue(fragmentId >= 0, "Label 'a' must map to a valid fragment ID");
  }

  @Test
  void classifier_returnsMinusOneForUnknownChar() {
    FragmentedAutomaton<String> fa = buildFragmented(Expression.unit(new IntRange(97, 97)));
    assertEquals(-1, fa.classifier().classify(122));
  }

  @Test
  void fragment_adjacentCharNotClassified() {
    // Single label 'b' (98). 97 must not be classified.
    FragmentedAutomaton<String> fa = buildFragmented(Expression.unit(new IntRange(98, 98)));
    assertEquals(-1, fa.classifier().classify(97), "'a' (97) must not be in 'b''s fragment");
  }

  @Test
  void twoDistinctLabels_mappedToDistinctFragments() {
    FragmentedAutomaton<String> fa =
        buildFragmented(
            Expression.choice(
                Expression.unit(new IntRange(97, 97)), Expression.unit(new IntRange(98, 98))));
    int fragA = fa.classifier().classify(97);
    int fragB = fa.classifier().classify(98);
    assertTrue(fragA >= 0);
    assertTrue(fragB >= 0);
    assertNotEquals(fragA, fragB, "'a' and 'b' must be in different fragments");
  }

  // --- Acceptance preserved ---

  @Test
  void unit_acceptsMatchingChar() {
    FragmentedAutomaton<String> fa = buildFragmented(Expression.unit(new IntRange(97, 97)));
    assertTrue(fragmentedAccepts(fa, List.of(97)));
    assertFalse(fragmentedAccepts(fa, List.of(98)));
    assertFalse(fragmentedAccepts(fa, List.of()));
  }

  @Test
  void sequence_acceptancePreserved() {
    FragmentedAutomaton<String> fa =
        buildFragmented(
            Expression.sequence(
                Expression.unit(new IntRange(97, 97)), Expression.unit(new IntRange(98, 98))));
    assertTrue(fragmentedAccepts(fa, List.of(97, 98)));
    assertFalse(fragmentedAccepts(fa, List.of(97)));
    assertFalse(fragmentedAccepts(fa, List.of(98, 97)));
  }

  @Test
  void choice_acceptancePreserved() {
    FragmentedAutomaton<String> fa =
        buildFragmented(
            Expression.choice(
                Expression.unit(new IntRange(97, 97)), Expression.unit(new IntRange(98, 98))));
    assertTrue(fragmentedAccepts(fa, List.of(97)));
    assertTrue(fragmentedAccepts(fa, List.of(98)));
    assertFalse(fragmentedAccepts(fa, List.of(99)));
  }

  @Test
  void star_acceptancePreserved() {
    FragmentedAutomaton<String> fa =
        buildFragmented(Expression.star(Expression.unit(new IntRange(97, 97))));
    assertTrue(fragmentedAccepts(fa, List.of()));
    assertTrue(fragmentedAccepts(fa, List.of(97)));
    assertTrue(fragmentedAccepts(fa, List.of(97, 97, 97)));
    assertFalse(fragmentedAccepts(fa, List.of(98)));
  }

  @Test
  void plus_acceptancePreserved() {
    FragmentedAutomaton<String> fa =
        buildFragmented(Expression.plus(Expression.unit(new IntRange(97, 97))));
    assertFalse(fragmentedAccepts(fa, List.of()));
    assertTrue(fragmentedAccepts(fa, List.of(97)));
    assertTrue(fragmentedAccepts(fa, List.of(97, 97)));
  }

  // --- Structural ---

  @Test
  void stateCountPreserved() {
    Expression<IntRange> expr =
        Expression.choice(
            Expression.unit(new IntRange(97, 97)), Expression.unit(new IntRange(98, 98)));
    Automaton<String, IntRange> nfa = ExpressionConverter.build(expr, "TOKEN");
    Automaton<String, IntRange> minDfa =
        Minimizer.minimize(RangeDeterminizer.determinize(nfa, Map.of()));
    FragmentedAutomaton<String> fa = AlphabetFragmenter.fragment(minDfa);
    assertEquals(minDfa.getStates().size(), fa.dfa().getStates().size());
  }

  @Test
  void noEpsilonTransitions() {
    FragmentedAutomaton<String> fa =
        buildFragmented(
            Expression.choice(
                Expression.unit(new IntRange(97, 97)), Expression.unit(new IntRange(98, 98))));
    for (State<String, Integer> state : fa.dfa().getStates()) {
      for (Transition<String, Integer> t : state.getTransitions()) {
        assertNotNull(t.label(), "Fragmented DFA must not have epsilon transitions");
      }
    }
  }

  // --- Range-specific ---

  @Test
  void fragment_singleRange_acceptsContainedChars() {
    FragmentedAutomaton<String> fa =
        buildFragmented(Expression.unit(new IntRange(97, 122))); // [a-z]
    assertTrue(fragmentedAccepts(fa, List.of(97))); // 'a'
    assertTrue(fragmentedAccepts(fa, List.of(122))); // 'z'
    assertFalse(fragmentedAccepts(fa, List.of(96))); // before 'a'
    assertFalse(fragmentedAccepts(fa, List.of(123))); // after 'z'
  }

  @Test
  void fragment_overlappingChoice_allCharsAccepted() {
    // [a-c] | [b-d] -- all of a,b,c,d must be accepted
    Expression<IntRange> expr =
        Expression.choice(
            Expression.unit(new IntRange(97, 99)), Expression.unit(new IntRange(98, 100)));
    FragmentedAutomaton<String> fa = buildFragmented(expr);
    for (int ch = 97; ch <= 100; ch++) {
      assertTrue(fragmentedAccepts(fa, List.of(ch)), "should accept " + (char) ch);
    }
    assertFalse(fragmentedAccepts(fa, List.of(96)));
    assertFalse(fragmentedAccepts(fa, List.of(101)));
  }

  // --- Fragment deduplication ---

  @Test
  void fragment_sameTransitionBehavior_sharedFragmentId() {
    // [a-z] -- all characters in range go to same target from same source.
    // After fragmentation, they should share a single fragment ID.
    FragmentedAutomaton<String> fa = buildFragmented(Expression.unit(new IntRange(97, 122)));
    int fragA = fa.classifier().classify(97);
    int fragZ = fa.classifier().classify(122);
    assertEquals(fragA, fragZ, "Characters with identical transitions should share a fragment");
  }
}
