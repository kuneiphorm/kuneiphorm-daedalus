package org.kuneiphorm.daedalus.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PairTest {

  @Test
  void first_returnsFirstElement() {
    Pair<String, Integer> pair = new Pair<>("hello", 42);
    assertEquals("hello", pair.first());
  }

  @Test
  void second_returnsSecondElement() {
    Pair<String, Integer> pair = new Pair<>("hello", 42);
    assertEquals(42, pair.second());
  }

  @Test
  void equals_sameValues_returnsTrue() {
    Pair<String, Integer> a = new Pair<>("hello", 42);
    Pair<String, Integer> b = new Pair<>("hello", 42);
    assertEquals(a, b);
  }

  @Test
  void equals_differentValues_returnsFalse() {
    Pair<String, Integer> a = new Pair<>("hello", 42);
    Pair<String, Integer> b = new Pair<>("world", 42);
    assertNotEquals(a, b);
  }

  @Test
  void toString_containsBothElements() {
    Pair<String, Integer> pair = new Pair<>("hello", 42);
    String str = pair.toString();
    assertTrue(str.contains("hello"));
    assertTrue(str.contains("42"));
  }
}
