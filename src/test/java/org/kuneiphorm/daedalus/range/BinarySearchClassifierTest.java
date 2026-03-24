package org.kuneiphorm.daedalus.range;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class BinarySearchClassifierTest {

  @Test
  void empty_returnsMinusOne() {
    var c = new BinarySearchClassifier(List.of());
    assertEquals(-1, c.classify(0));
  }

  @Test
  void singleRange_belowLo() {
    var c = new BinarySearchClassifier(List.of(new IntRange(5, 10)));
    assertEquals(-1, c.classify(4));
  }

  @Test
  void singleRange_atLo() {
    var c = new BinarySearchClassifier(List.of(new IntRange(5, 10)));
    assertEquals(0, c.classify(5));
  }

  @Test
  void singleRange_within() {
    var c = new BinarySearchClassifier(List.of(new IntRange(5, 10)));
    assertEquals(0, c.classify(7));
  }

  @Test
  void singleRange_atHi() {
    var c = new BinarySearchClassifier(List.of(new IntRange(5, 10)));
    assertEquals(0, c.classify(10));
  }

  @Test
  void singleRange_aboveHi() {
    var c = new BinarySearchClassifier(List.of(new IntRange(5, 10)));
    assertEquals(-1, c.classify(11));
  }

  @Test
  void multipleRanges_inFirstRange() {
    var c = new BinarySearchClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(0, c.classify(2));
  }

  @Test
  void multipleRanges_inGap() {
    var c = new BinarySearchClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(-1, c.classify(5));
  }

  @Test
  void multipleRanges_inSecondRange() {
    var c = new BinarySearchClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(1, c.classify(8));
  }

  @Test
  void multipleRanges_aboveAll() {
    var c = new BinarySearchClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(-1, c.classify(100));
  }

  @Test
  void multipleRanges_belowAll() {
    var c = new BinarySearchClassifier(List.of(new IntRange(10, 20), new IntRange(30, 40)));
    assertEquals(-1, c.classify(5));
  }

  @Test
  void threeRanges_middleRange() {
    var c =
        new BinarySearchClassifier(
            List.of(new IntRange(1, 3), new IntRange(10, 15), new IntRange(20, 25)));
    assertEquals(1, c.classify(12));
  }

  @Test
  void fragmentCount_empty_returnsZero() {
    var c = new BinarySearchClassifier(List.of());
    assertEquals(0, c.fragmentCount());
  }

  @Test
  void fragmentCount_returnsNumberOfRanges() {
    var c = new BinarySearchClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(2, c.fragmentCount());
  }

  @Test
  void getRanges_returnsRanges() {
    var ranges = List.of(new IntRange(1, 3));
    var c = new BinarySearchClassifier(ranges);
    assertEquals(ranges, c.getRanges());
  }
}
