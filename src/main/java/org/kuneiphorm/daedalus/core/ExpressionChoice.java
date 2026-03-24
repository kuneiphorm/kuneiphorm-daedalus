package org.kuneiphorm.daedalus.core;

import java.util.List;

/**
 * An expression that matches if any one of its alternatives matches (alternation, {@code a | b}).
 *
 * <p>An empty choice represents the empty language -- it matches nothing.
 *
 * @param <L> the label type of {@link ExpressionUnit} leaves
 * @param alternatives the alternative expressions
 * @author Florent Guille
 * @since 0.1.0
 */
public record ExpressionChoice<L>(List<Expression<L>> alternatives) implements Expression<L> {

  /** Creates a choice, defensively copying the alternatives list. */
  public ExpressionChoice {
    alternatives = List.copyOf(alternatives);
  }

  @Override
  public List<Expression<L>> getChildren() {
    return alternatives;
  }

  @Override
  public String getSignature() {
    return "Choice(" + alternatives.size() + ")";
  }
}
