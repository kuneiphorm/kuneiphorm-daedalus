package org.kuneiphorm.daedalus.range;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinearClassifierTest {

  @Test
  void empty_returnsMinusOne() {
    var c = new LinearClassifier(List.of());
    assertEquals(-1, c.classify(0));
  }

  @Test
  void singleRange_belowLo() {
    var c = new LinearClassifier(List.of(new IntRange(5, 10)));
    assertEquals(-1, c.classify(4));
  }

  @Test
  void singleRange_atLo() {
    var c = new LinearClassifier(List.of(new IntRange(5, 10)));
    assertEquals(0, c.classify(5));
  }

  @Test
  void singleRange_within() {
    var c = new LinearClassifier(List.of(new IntRange(5, 10)));
    assertEquals(0, c.classify(7));
  }

  @Test
  void singleRange_atHi() {
    var c = new LinearClassifier(List.of(new IntRange(5, 10)));
    assertEquals(0, c.classify(10));
  }

  @Test
  void singleRange_aboveHi() {
    var c = new LinearClassifier(List.of(new IntRange(5, 10)));
    assertEquals(-1, c.classify(11));
  }

  @Test
  void multipleRanges_inFirstRange() {
    var c = new LinearClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(0, c.classify(2));
  }

  @Test
  void multipleRanges_inGap() {
    var c = new LinearClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(-1, c.classify(5));
  }

  @Test
  void multipleRanges_inSecondRange() {
    var c = new LinearClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(1, c.classify(8));
  }

  @Test
  void multipleRanges_aboveAll() {
    var c = new LinearClassifier(List.of(new IntRange(1, 3), new IntRange(7, 9)));
    assertEquals(-1, c.classify(100));
  }

  @Test
  void getRanges_returnsRanges() {
    var ranges = List.of(new IntRange(1, 3));
    var c = new LinearClassifier(ranges);
    assertEquals(ranges, c.getRanges());
  }
}
