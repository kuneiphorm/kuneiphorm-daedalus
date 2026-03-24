package org.kuneiphorm.daedalus.craft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.core.Pair;
import org.kuneiphorm.daedalus.range.IntRange;

class RangeDeterminizerTest {

  // Simulate a DFA on a sequence of integer inputs.
  private static <S> boolean dfaAccepts(Automaton<S, IntRange> dfa, List<Integer> input) {
    var current = dfa.getInitial();
    for (int ch : input) {
      State<S, IntRange> next = null;
      for (var t : current.getTransitions()) {
        if (t.label().lo() <= ch && ch <= t.label().hi()) {
          next = t.target();
          break;
        }
      }
      if (next == null) return false;
      current = next;
    }
    return current.isAccepting();
  }

  // Build an NFA from an Expression<IntRange> (via ExpressionConverter), determinize it,
  // and return the resulting DFA.
  private static Automaton<String, IntRange> buildDfa(
      org.kuneiphorm.daedalus.core.Expression<IntRange> expr) {
    var nfa = ExpressionConverter.build(expr, "TOKEN");
    return RangeDeterminizer.determinize(nfa, Map.of());
  }

  // --- Single range ---

  @Test
  void singleRange_acceptsContainedChar() {
    // [a-z] -- accept any char in 97..122
    var dfa = buildDfa(org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(97, 122)));
    assertTrue(dfaAccepts(dfa, List.of(97))); // 'a'
    assertTrue(dfaAccepts(dfa, List.of(122))); // 'z'
    assertFalse(dfaAccepts(dfa, List.of(96))); // before 'a'
    assertFalse(dfaAccepts(dfa, List.of(123))); // after 'z'
    assertFalse(dfaAccepts(dfa, List.of())); // empty
  }

  // --- Non-overlapping choice ---

  @Test
  void nonOverlappingChoice_acceptsBothRanges() {
    // [a-z] | [0-9]
    var expr =
        org.kuneiphorm.daedalus.core.Expression.choice(
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(97, 122)),
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(48, 57)));
    var dfa = buildDfa(expr);
    assertTrue(dfaAccepts(dfa, List.of(97))); // 'a'
    assertTrue(dfaAccepts(dfa, List.of(48))); // '0'
    assertFalse(dfaAccepts(dfa, List.of(65))); // 'A'
  }

  // --- Overlapping ranges in a choice (two NFA branches covering same chars) ---

  @Test
  void overlappingRanges_noDuplicateTransitions() {
    // [a-c] | [b-d] -- overlap at b, c
    var expr =
        org.kuneiphorm.daedalus.core.Expression.choice(
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(97, 99)), // a-c
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(98, 100))); // b-d
    var dfa = buildDfa(expr);
    // All of a,b,c,d should be accepted; each DFA transition must be non-overlapping.
    for (int ch = 97; ch <= 100; ch++) {
      assertTrue(dfaAccepts(dfa, List.of(ch)), "should accept " + (char) ch);
    }
    assertFalse(dfaAccepts(dfa, List.of(96))); // before 'a'
    assertFalse(dfaAccepts(dfa, List.of(101))); // after 'd'
    // Verify non-overlapping: for each DFA state, no two transitions cover the same integer.
    for (var state : dfa.getStates()) {
      var transitions = state.getTransitions();
      for (int i = 0; i < transitions.size(); i++) {
        for (int j = i + 1; j < transitions.size(); j++) {
          var ri = transitions.get(i).label();
          var rj = transitions.get(j).label();
          assertFalse(
              ri.lo() <= rj.hi() && rj.lo() <= ri.hi(), "Overlapping transitions in DFA state");
        }
      }
    }
  }

  // --- Priority resolution ---

  @Test
  void priorityResolution_firstRuleWins() {
    // Build a combined NFA manually: two accepting states for the same range, different outputs.
    var nfa = Automaton.<String, IntRange>create();
    var root = nfa.newState();
    nfa.setInitialStateId(root.getId());

    // Rule 0: [a] → "RULE0"
    var s0 = nfa.newState();
    root.addEpsilonTransition(s0);
    s0.addTransition(new IntRange(97, 97), nfa.newState("RULE0"));

    // Rule 1: [a] → "RULE1"
    var s1 = nfa.newState();
    root.addEpsilonTransition(s1);
    s1.addTransition(new IntRange(97, 97), nfa.newState("RULE1"));

    // Priority: "RULE0" has priority 0 (higher), "RULE1" has priority 1 (lower).
    var priority = Map.of("RULE0", 0, "RULE1", 1);
    var dfa = RangeDeterminizer.determinize(nfa, priority);

    // The accepting state reachable via 'a' should output "RULE0".
    var current = dfa.getInitial();
    State<String, IntRange> next = null;
    for (var t : current.getTransitions()) {
      if (t.label().lo() <= 97 && 97 <= t.label().hi()) {
        next = t.target();
        break;
      }
    }
    assertNotNull(next);
    assertEquals("RULE0", next.getOutput());
  }

  // --- Epsilon closure ---

  @Test
  void epsilonTransitions_resolvedCorrectly() {
    // NFA: root --eps--> s1 --[a]--> accept
    var nfa = Automaton.<String, IntRange>create();
    var root = nfa.newState();
    nfa.setInitialStateId(root.getId());
    var s1 = nfa.newState();
    var accept = nfa.newState("OK");
    root.addEpsilonTransition(s1);
    s1.addTransition(new IntRange(97, 97), accept);

    var dfa = RangeDeterminizer.determinize(nfa, Map.of());
    assertTrue(dfaAccepts(dfa, List.of(97)));
    assertFalse(dfaAccepts(dfa, List.of(98)));
  }

  // --- Sequence ---

  @Test
  void sequence_acceptancePreserved() {
    // [a][b]
    var expr =
        org.kuneiphorm.daedalus.core.Expression.sequence(
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(97, 97)),
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(98, 98)));
    var dfa = buildDfa(expr);
    assertTrue(dfaAccepts(dfa, List.of(97, 98)));
    assertFalse(dfaAccepts(dfa, List.of(97)));
    assertFalse(dfaAccepts(dfa, List.of(98, 97)));
  }

  // --- Star ---

  @Test
  void star_acceptancePreserved() {
    var expr =
        org.kuneiphorm.daedalus.core.Expression.star(
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(97, 97)));
    var dfa = buildDfa(expr);
    assertTrue(dfaAccepts(dfa, List.of()));
    assertTrue(dfaAccepts(dfa, List.of(97)));
    assertTrue(dfaAccepts(dfa, List.of(97, 97, 97)));
    assertFalse(dfaAccepts(dfa, List.of(97, 98)));
  }

  // --- Direct sweepLine tests ---

  private static Pair<IntRange, Integer> entry(int lo, int hi, int target) {
    return new Pair<>(new IntRange(lo, hi), target);
  }

  @Test
  void sweepLine_emptyTransitions_returnsEmpty() {
    List<Pair<IntRange, Set<Integer>>> result = RangeDeterminizer.sweepLine(List.of());
    assertTrue(result.isEmpty());
  }

  @Test
  void sweepLine_singleRange_singlePartition() {
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(10, 20, 0)));
    assertEquals(1, result.size());
    assertEquals(new Pair<>(new IntRange(10, 20), Set.of(0)), result.get(0));
  }

  @Test
  void sweepLine_nonOverlappingRanges_separatePartitions() {
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(1, 5, 0), entry(10, 15, 1)));
    assertEquals(2, result.size());
    assertEquals(new Pair<>(new IntRange(1, 5), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(10, 15), Set.of(1)), result.get(1));
  }

  @Test
  void sweepLine_overlappingRanges_splitAtBoundaries() {
    // [1..5] target 0, [3..7] target 1
    // Expected: [1,2]→{0}, [3,5]→{0,1}, [6,7]→{1}
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(1, 5, 0), entry(3, 7, 1)));
    assertEquals(3, result.size());
    assertEquals(new Pair<>(new IntRange(1, 2), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(3, 5), Set.of(0, 1)), result.get(1));
    assertEquals(new Pair<>(new IntRange(6, 7), Set.of(1)), result.get(2));
  }

  @Test
  void sweepLine_adjacentRanges_noOverlap() {
    // [1..5] target 0, [6..10] target 1 -- share boundary at 5/6
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(1, 5, 0), entry(6, 10, 1)));
    assertEquals(2, result.size());
    assertEquals(new Pair<>(new IntRange(1, 5), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(6, 10), Set.of(1)), result.get(1));
  }

  @Test
  void sweepLine_sameRangeDifferentTargets_merged() {
    // [1..5] target 0 and [1..5] target 1 -- identical range, different targets
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(1, 5, 0), entry(1, 5, 1)));
    assertEquals(1, result.size());
    assertEquals(new Pair<>(new IntRange(1, 5), Set.of(0, 1)), result.get(0));
  }

  @Test
  void sweepLine_duplicateRangeSameTarget_singlePartition() {
    // [1..5] target 0 appears twice -- reference counting should handle this
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(1, 5, 0), entry(1, 5, 0)));
    assertEquals(1, result.size());
    assertEquals(new Pair<>(new IntRange(1, 5), Set.of(0)), result.get(0));
  }

  @Test
  void sweepLine_touchingRangesAtSamePoint_correctOverlap() {
    // [1..5] target 0 and [5..10] target 1 -- they share the exact point 5.
    // Expected: [1,4]→{0}, [5,5]→{0,1}, [6,10]→{1}
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(1, 5, 0), entry(5, 10, 1)));
    assertEquals(3, result.size());
    assertEquals(new Pair<>(new IntRange(1, 4), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(5, 5), Set.of(0, 1)), result.get(1));
    assertEquals(new Pair<>(new IntRange(6, 10), Set.of(1)), result.get(2));
  }

  @Test
  void sweepLine_nestedRanges_correctSplit() {
    // [1..10] target 0 contains [3..7] target 1
    // Expected: [1,2]→{0}, [3,7]→{0,1}, [8,10]→{0}
    List<Pair<IntRange, Set<Integer>>> result =
        RangeDeterminizer.sweepLine(List.of(entry(1, 10, 0), entry(3, 7, 1)));
    assertEquals(3, result.size());
    assertEquals(new Pair<>(new IntRange(1, 2), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(3, 7), Set.of(0, 1)), result.get(1));
    assertEquals(new Pair<>(new IntRange(8, 10), Set.of(0)), result.get(2));
  }

  // --- DFA is deterministic ---

  @Test
  void resultIsDeterministic() {
    var expr =
        org.kuneiphorm.daedalus.core.Expression.choice(
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(97, 100)),
            org.kuneiphorm.daedalus.core.Expression.unit(new IntRange(98, 102)));
    var dfa = buildDfa(expr);
    // Each state must have at most one transition per integer value in the covered range.
    for (var state : dfa.getStates()) {
      var transitions = state.getTransitions();
      for (int i = 0; i < transitions.size(); i++) {
        for (int j = i + 1; j < transitions.size(); j++) {
          var ri = transitions.get(i).label();
          var rj = transitions.get(j).label();
          assertFalse(
              ri.lo() <= rj.hi() && rj.lo() <= ri.hi(),
              "DFA has overlapping transitions -- not deterministic");
        }
      }
    }
  }
}
