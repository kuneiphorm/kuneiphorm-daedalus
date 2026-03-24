package org.kuneiphorm.daedalus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ExpressionQuantifierTest {

  private final Expression<String> body = Expression.unit("A");

  @Test
  public void getSignature_optional() {
    assertEquals("Quantifier(?)", Expression.optional(body).getSignature());
  }

  @Test
  public void getSignature_star() {
    assertEquals("Quantifier(*)", Expression.star(body).getSignature());
  }

  @Test
  public void getSignature_plus() {
    assertEquals("Quantifier(+)", Expression.plus(body).getSignature());
  }

  @Test
  public void getChildren() {
    assertEquals(List.of(body), Expression.optional(body).getChildren());
  }

  @Test
  public void kind_optional() {
    assertEquals(ExpressionQuantifier.Kind.OPTIONAL, Expression.optional(body).kind());
  }

  @Test
  public void kind_star() {
    assertEquals(ExpressionQuantifier.Kind.STAR, Expression.star(body).kind());
  }

  @Test
  public void kind_plus() {
    assertEquals(ExpressionQuantifier.Kind.PLUS, Expression.plus(body).kind());
  }
}
