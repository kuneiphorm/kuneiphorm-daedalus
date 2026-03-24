package org.kuneiphorm.daedalus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ExpressionUnitTest {

  @Test
  public void getSignature() {
    assertEquals("Unit(A)", Expression.unit("A").getSignature());
  }

  @Test
  public void getChildren() {
    assertEquals(List.of(), Expression.unit("A").getChildren());
  }

  @Test
  public void labelIsPreserved() {
    Object label = new Object();
    assertSame(label, Expression.unit(label).label());
  }
}
