package org.kuneiphorm.daedalus.core;

import java.util.List;

/**
 * A leaf expression that matches a single atomic element identified by a label.
 *
 * <p>The meaning of the label is defined by the consuming module (e.g. a character class in {@code
 * kuneiphorm-regex}, a terminal or non-terminal symbol in {@code kuneiphorm-grammar}). Daedalus
 * treats the label as opaque.
 *
 * @param <L> the label type
 * @param label the opaque label for this unit
 * @author Florent Guille
 * @since 0.1.0
 */
public record ExpressionUnit<L>(L label) implements Expression<L> {

  @Override
  public List<Expression<L>> getChildren() {
    return List.of();
  }

  @Override
  public String getSignature() {
    return "Unit(" + label + ")";
  }
}
