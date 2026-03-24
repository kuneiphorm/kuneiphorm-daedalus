package org.kuneiphorm.daedalus.range;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Pair;

class IntRangeTest {

  @Test
  void constructor_valid() {
    IntRange r = new IntRange(3, 7);
    assertEquals(3, r.lo());
    assertEquals(7, r.hi());
  }

  @Test
  void constructor_singlePoint() {
    IntRange r = new IntRange(5, 5);
    assertEquals(5, r.lo());
    assertEquals(5, r.hi());
  }

  @Test
  void constructor_loGreaterThanHi_throws() {
    assertThrows(IllegalArgumentException.class, () -> new IntRange(10, 5));
  }

  @Test
  void contains_below() {
    assertFalse(new IntRange(3, 7).contains(2));
  }

  @Test
  void contains_atLo() {
    assertTrue(new IntRange(3, 7).contains(3));
  }

  @Test
  void contains_within() {
    assertTrue(new IntRange(3, 7).contains(5));
  }

  @Test
  void contains_atHi() {
    assertTrue(new IntRange(3, 7).contains(7));
  }

  @Test
  void contains_above() {
    assertFalse(new IntRange(3, 7).contains(8));
  }

  // --- overlaps ---

  @Test
  void overlaps_disjointBefore_returnsFalse() {
    assertFalse(new IntRange(1, 3).overlaps(new IntRange(5, 7)));
  }

  @Test
  void overlaps_disjointAfter_returnsFalse() {
    assertFalse(new IntRange(5, 7).overlaps(new IntRange(1, 3)));
  }

  @Test
  void overlaps_adjacent_returnsFalse() {
    assertFalse(new IntRange(1, 3).overlaps(new IntRange(4, 7)));
  }

  @Test
  void overlaps_touchingAtHi_returnsTrue() {
    assertTrue(new IntRange(1, 4).overlaps(new IntRange(4, 7)));
  }

  @Test
  void overlaps_touchingAtLo_returnsTrue() {
    assertTrue(new IntRange(4, 7).overlaps(new IntRange(1, 4)));
  }

  @Test
  void overlaps_partial_returnsTrue() {
    assertTrue(new IntRange(1, 5).overlaps(new IntRange(3, 7)));
  }

  @Test
  void overlaps_nested_returnsTrue() {
    assertTrue(new IntRange(1, 10).overlaps(new IntRange(3, 7)));
  }

  @Test
  void overlaps_identical_returnsTrue() {
    assertTrue(new IntRange(3, 7).overlaps(new IntRange(3, 7)));
  }

  @Test
  void overlaps_symmetric() {
    IntRange a = new IntRange(1, 5);
    IntRange b = new IntRange(3, 7);
    assertEquals(a.overlaps(b), b.overlaps(a));
  }

  // --- compareTo ---

  @Test
  void compareTo_sameLo_orderedByHi() {
    IntRange a = new IntRange(1, 5);
    IntRange b = new IntRange(1, 10);
    assertTrue(a.compareTo(b) < 0);
    assertTrue(b.compareTo(a) > 0);
  }

  @Test
  void compareTo_differentLo_orderedByLo() {
    IntRange a = new IntRange(1, 10);
    IntRange b = new IntRange(5, 7);
    assertTrue(a.compareTo(b) < 0);
    assertTrue(b.compareTo(a) > 0);
  }

  @Test
  void compareTo_equal_returnsZero() {
    IntRange a = new IntRange(3, 7);
    IntRange b = new IntRange(3, 7);
    assertEquals(0, a.compareTo(b));
  }

  // --- normalize ---

  @Test
  void normalize_empty_returnsEmpty() {
    assertEquals(List.of(), IntRange.normalize(List.of()));
  }

  @Test
  void normalize_single_returnsSame() {
    assertEquals(List.of(new IntRange(1, 5)), IntRange.normalize(List.of(new IntRange(1, 5))));
  }

  @Test
  void normalize_noOverlap_returnsSorted() {
    List<IntRange> result = IntRange.normalize(List.of(new IntRange(10, 15), new IntRange(1, 5)));
    assertEquals(List.of(new IntRange(1, 5), new IntRange(10, 15)), result);
  }

  @Test
  void normalize_overlapping_merges() {
    List<IntRange> result = IntRange.normalize(List.of(new IntRange(1, 5), new IntRange(3, 7)));
    assertEquals(List.of(new IntRange(1, 7)), result);
  }

  @Test
  void normalize_adjacent_merges() {
    List<IntRange> result = IntRange.normalize(List.of(new IntRange(1, 5), new IntRange(6, 10)));
    assertEquals(List.of(new IntRange(1, 10)), result);
  }

  @Test
  void normalize_nested_merges() {
    List<IntRange> result = IntRange.normalize(List.of(new IntRange(1, 10), new IntRange(3, 7)));
    assertEquals(List.of(new IntRange(1, 10)), result);
  }

  @Test
  void normalize_multipleOverlapping_mergesAll() {
    List<IntRange> result =
        IntRange.normalize(List.of(new IntRange(1, 3), new IntRange(2, 5), new IntRange(4, 7)));
    assertEquals(List.of(new IntRange(1, 7)), result);
  }

  // --- negate ---

  @Test
  void negate_empty_returnsFullDomain() {
    assertEquals(List.of(new IntRange(0, 100)), IntRange.negate(List.of(), 0, 100));
  }

  @Test
  void negate_fullDomain_returnsEmpty() {
    assertEquals(List.of(), IntRange.negate(List.of(new IntRange(0, 100)), 0, 100));
  }

  @Test
  void negate_gapBefore() {
    List<IntRange> result = IntRange.negate(List.of(new IntRange(5, 10)), 0, 10);
    assertEquals(List.of(new IntRange(0, 4)), result);
  }

  @Test
  void negate_gapAfter() {
    List<IntRange> result = IntRange.negate(List.of(new IntRange(0, 5)), 0, 10);
    assertEquals(List.of(new IntRange(6, 10)), result);
  }

  @Test
  void negate_gapBetween() {
    List<IntRange> result =
        IntRange.negate(List.of(new IntRange(0, 3), new IntRange(7, 10)), 0, 10);
    assertEquals(List.of(new IntRange(4, 6)), result);
  }

  @Test
  void negate_multipleGaps() {
    List<IntRange> result = IntRange.negate(List.of(new IntRange(2, 3), new IntRange(6, 7)), 0, 10);
    assertEquals(List.of(new IntRange(0, 1), new IntRange(4, 5), new IntRange(8, 10)), result);
  }

  // --- partition ---

  private static Pair<IntRange, Set<Integer>> labeled(int lo, int hi, Integer... labels) {
    Set<Integer> set = new java.util.HashSet<>();
    for (Integer label : labels) {
      set.add(label);
    }
    return new Pair<>(new IntRange(lo, hi), set);
  }

  @Test
  void partition_empty_returnsEmpty() {
    List<Pair<IntRange, Set<Integer>>> result = IntRange.partition(List.of());
    assertTrue(result.isEmpty());
  }

  @Test
  void partition_single_returnsSame() {
    Pair<IntRange, Set<Integer>> range = labeled(1, 5, 0);
    List<Pair<IntRange, Set<Integer>>> result = IntRange.partition(List.of(range));
    assertEquals(1, result.size());
    assertEquals(range, result.get(0));
  }

  @Test
  void partition_nonOverlapping_separateRanges() {
    List<Pair<IntRange, Set<Integer>>> result =
        IntRange.partition(List.of(labeled(1, 5, 0), labeled(10, 15, 1)));
    assertEquals(2, result.size());
    assertEquals(new Pair<>(new IntRange(1, 5), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(10, 15), Set.of(1)), result.get(1));
  }

  @Test
  void partition_overlapping_splitsAtBoundaries() {
    // [1..5] label {0}, [3..7] label {1}
    // Expected: [1,2]→{0}, [3,5]→{0,1}, [6,7]→{1}
    List<Pair<IntRange, Set<Integer>>> result =
        IntRange.partition(List.of(labeled(1, 5, 0), labeled(3, 7, 1)));
    assertEquals(3, result.size());
    assertEquals(new Pair<>(new IntRange(1, 2), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(3, 5), Set.of(0, 1)), result.get(1));
    assertEquals(new Pair<>(new IntRange(6, 7), Set.of(1)), result.get(2));
  }

  @Test
  void partition_sameRangeDifferentLabels_merged() {
    List<Pair<IntRange, Set<Integer>>> result =
        IntRange.partition(List.of(labeled(1, 5, 0), labeled(1, 5, 1)));
    assertEquals(1, result.size());
    assertEquals(new Pair<>(new IntRange(1, 5), Set.of(0, 1)), result.get(0));
  }

  @Test
  void partition_duplicateRangeSameLabel_singlePartition() {
    List<Pair<IntRange, Set<Integer>>> result =
        IntRange.partition(List.of(labeled(1, 5, 0), labeled(1, 5, 0)));
    assertEquals(1, result.size());
    assertEquals(new Pair<>(new IntRange(1, 5), Set.of(0)), result.get(0));
  }

  @Test
  void partition_nested_correctSplit() {
    // [1..10] label {0} contains [3..7] label {1}
    // Expected: [1,2]→{0}, [3,7]→{0,1}, [8,10]→{0}
    List<Pair<IntRange, Set<Integer>>> result =
        IntRange.partition(List.of(labeled(1, 10, 0), labeled(3, 7, 1)));
    assertEquals(3, result.size());
    assertEquals(new Pair<>(new IntRange(1, 2), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(3, 7), Set.of(0, 1)), result.get(1));
    assertEquals(new Pair<>(new IntRange(8, 10), Set.of(0)), result.get(2));
  }

  @Test
  void partition_touching_correctOverlap() {
    // [1..5] label {0} and [5..10] label {1} -- share point 5
    // Expected: [1,4]→{0}, [5,5]→{0,1}, [6,10]→{1}
    List<Pair<IntRange, Set<Integer>>> result =
        IntRange.partition(List.of(labeled(1, 5, 0), labeled(5, 10, 1)));
    assertEquals(3, result.size());
    assertEquals(new Pair<>(new IntRange(1, 4), Set.of(0)), result.get(0));
    assertEquals(new Pair<>(new IntRange(5, 5), Set.of(0, 1)), result.get(1));
    assertEquals(new Pair<>(new IntRange(6, 10), Set.of(1)), result.get(2));
  }
}
