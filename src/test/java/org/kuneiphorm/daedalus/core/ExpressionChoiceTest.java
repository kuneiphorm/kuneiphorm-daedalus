package org.kuneiphorm.daedalus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ExpressionChoiceTest {

  @Test
  public void getSignature() {
    var choice =
        Expression.choice(Expression.unit("A"), Expression.unit("B"), Expression.unit("C"));
    assertEquals("Choice(3)", choice.getSignature());
  }

  @Test
  public void getSignature_empty() {
    assertEquals("Choice(0)", Expression.choice().getSignature());
  }

  @Test
  public void getChildren() {
    List<Expression<String>> children =
        List.of(Expression.unit("A"), Expression.unit("B"), Expression.unit("C"));
    assertEquals(children, new ExpressionChoice<>(children).getChildren());
  }

  @Test
  public void immutable() {
    var a = Expression.unit("A");
    var mutable = new ArrayList<Expression<String>>(List.of(a));
    var choice = new ExpressionChoice<>(mutable);
    mutable.add(Expression.unit("B"));
    assertEquals(1, choice.alternatives().size());
  }
}
