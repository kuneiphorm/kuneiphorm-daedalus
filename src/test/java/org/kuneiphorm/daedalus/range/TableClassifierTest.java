package org.kuneiphorm.daedalus.range;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TableClassifierTest {

  @Test
  void classify_inRange() {
    var c = new TableClassifier(List.of(new IntRange(3, 5)), 10);
    assertEquals(0, c.classify(3));
    assertEquals(0, c.classify(4));
    assertEquals(0, c.classify(5));
  }

  @Test
  void classify_belowRange_returnsMinusOne() {
    var c = new TableClassifier(List.of(new IntRange(3, 5)), 10);
    assertEquals(-1, c.classify(2));
  }

  @Test
  void classify_aboveRange_withinTable_returnsMinusOne() {
    var c = new TableClassifier(List.of(new IntRange(3, 5)), 10);
    assertEquals(-1, c.classify(6));
  }

  @Test
  void classify_negativeInput_returnsMinusOne() {
    var c = new TableClassifier(List.of(new IntRange(0, 5)), 10);
    assertEquals(-1, c.classify(-1));
  }

  @Test
  void classify_atTableSize_returnsMinusOne() {
    var c = new TableClassifier(List.of(new IntRange(0, 5)), 10);
    assertEquals(-1, c.classify(10));
  }

  @Test
  void classify_aboveTableSize_returnsMinusOne() {
    var c = new TableClassifier(List.of(new IntRange(0, 5)), 10);
    assertEquals(-1, c.classify(100));
  }

  @Test
  void multipleRanges_correctClassIds() {
    var c = new TableClassifier(List.of(new IntRange(0, 2), new IntRange(5, 7)), 10);
    assertEquals(0, c.classify(0));
    assertEquals(0, c.classify(2));
    assertEquals(-1, c.classify(3));
    assertEquals(1, c.classify(5));
    assertEquals(1, c.classify(7));
  }

  @Test
  void fragmentCount_returnsNumberOfRanges() {
    var c = new TableClassifier(List.of(new IntRange(0, 2), new IntRange(5, 7)), 10);
    assertEquals(2, c.fragmentCount());
  }

  @Test
  void fragmentCount_empty_returnsZero() {
    var c = new TableClassifier(List.of(), 10);
    assertEquals(0, c.fragmentCount());
  }

  @Test
  void constructor_rangeExceedsTableSize_throws() {
    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new TableClassifier(List.of(new IntRange(0, 10)), 10));
    assertTrue(ex.getMessage().contains("9"), "Message should contain tableSize - 1 (9)");
  }
}
