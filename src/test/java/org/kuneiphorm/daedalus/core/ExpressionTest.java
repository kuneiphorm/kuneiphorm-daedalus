package org.kuneiphorm.daedalus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ExpressionTest {

  // -------------------------------------------------------------------------
  // unfoldPrefix
  // -------------------------------------------------------------------------

  @Test
  public void unfoldPrefix_unit() {
    var a = Expression.unit("A");
    assertEquals(List.of(a), Expression.unfoldPrefix(a).toList());
  }

  @Test
  public void unfoldPrefix_flat() {
    // Choice(A, B) → [Choice, A, B]
    var a = Expression.unit("A");
    var b = Expression.unit("B");
    var choice = Expression.choice(a, b);
    assertEquals(List.of(choice, a, b), Expression.unfoldPrefix(choice).toList());
  }

  @Test
  public void unfoldPrefix_nested() {
    // Choice(Sequence(A, B), C) → [Choice, Sequence, A, B, C]
    var a = Expression.unit("A");
    var b = Expression.unit("B");
    var c = Expression.unit("C");
    var seq = Expression.sequence(a, b);
    var choice = Expression.choice(seq, c);
    assertEquals(List.of(choice, seq, a, b, c), Expression.unfoldPrefix(choice).toList());
  }

  @Test
  public void unfoldPrefix_emptyChoice() {
    var choice = Expression.<String>choice();
    assertEquals(List.of(choice), Expression.unfoldPrefix(choice).toList());
  }

  // -------------------------------------------------------------------------
  // unfoldPostfix
  // -------------------------------------------------------------------------

  @Test
  public void unfoldPostfix_unit() {
    var a = Expression.unit("A");
    assertEquals(List.of(a), Expression.unfoldPostfix(a).toList());
  }

  @Test
  public void unfoldPostfix_flat() {
    // Choice(A, B) → [A, B, Choice]
    var a = Expression.unit("A");
    var b = Expression.unit("B");
    var choice = Expression.choice(a, b);
    assertEquals(List.of(a, b, choice), Expression.unfoldPostfix(choice).toList());
  }

  @Test
  public void unfoldPostfix_nested() {
    // Choice(Sequence(A, B), C) → [A, B, Sequence, C, Choice]
    var a = Expression.unit("A");
    var b = Expression.unit("B");
    var c = Expression.unit("C");
    var seq = Expression.sequence(a, b);
    var choice = Expression.choice(seq, c);
    assertEquals(List.of(a, b, seq, c, choice), Expression.unfoldPostfix(choice).toList());
  }

  @Test
  public void unfoldPostfix_emptyChoice() {
    var choice = Expression.<String>choice();
    assertEquals(List.of(choice), Expression.unfoldPostfix(choice).toList());
  }

  // -------------------------------------------------------------------------
  // Package-private list overloads
  // -------------------------------------------------------------------------

  @Test
  public void choiceList_factory() {
    var a = Expression.unit("A");
    var b = Expression.unit("B");
    var choice = Expression.choice(List.of(a, b));
    assertEquals(List.of(a, b), choice.alternatives());
  }

  @Test
  public void sequenceList_factory() {
    var a = Expression.unit("A");
    var b = Expression.unit("B");
    var seq = Expression.sequence(List.of(a, b));
    assertEquals(List.of(a, b), seq.elements());
  }
}
