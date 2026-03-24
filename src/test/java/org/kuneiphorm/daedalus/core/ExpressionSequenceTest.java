package org.kuneiphorm.daedalus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ExpressionSequenceTest {

  @Test
  public void getSignature() {
    var seq = Expression.sequence(Expression.unit("A"), Expression.unit("B"));
    assertEquals("Sequence(2)", seq.getSignature());
  }

  @Test
  public void getSignature_empty() {
    assertEquals("Sequence(0)", Expression.sequence().getSignature());
  }

  @Test
  public void getChildren() {
    List<Expression<String>> elements =
        List.of(Expression.unit("A"), Expression.unit("B"), Expression.unit("C"));
    assertEquals(elements, new ExpressionSequence<>(elements).getChildren());
  }

  @Test
  public void immutable() {
    var a = Expression.unit("A");
    var mutable = new ArrayList<Expression<String>>(List.of(a));
    var seq = new ExpressionSequence<>(mutable);
    mutable.add(Expression.unit("B"));
    assertEquals(1, seq.elements().size());
  }
}
