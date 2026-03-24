package org.kuneiphorm.daedalus.core;

import java.util.List;

/**
 * An expression that matches its elements one after another (concatenation, {@code ab}).
 *
 * <p>An empty sequence represents the empty string (epsilon) -- it always matches.
 *
 * @param <L> the label type of {@link ExpressionUnit} leaves
 * @param elements the elements to concatenate
 * @author Florent Guille
 * @since 0.1.0
 */
public record ExpressionSequence<L>(List<Expression<L>> elements) implements Expression<L> {

  /** Creates a sequence, defensively copying the elements list. */
  public ExpressionSequence {
    elements = List.copyOf(elements);
  }

  @Override
  public List<Expression<L>> getChildren() {
    return elements;
  }

  @Override
  public String getSignature() {
    return "Sequence(" + elements.size() + ")";
  }
}
