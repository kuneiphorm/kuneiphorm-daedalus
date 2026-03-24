package org.kuneiphorm.daedalus.core;

import java.util.List;

/**
 * An expression that applies a classic regex quantifier to its body.
 *
 * <ul>
 *   <li>{@link Kind#OPTIONAL} -- {@code body?}: zero or one occurrence
 *   <li>{@link Kind#STAR} -- {@code body*}: zero or more occurrences
 *   <li>{@link Kind#PLUS} -- {@code body+}: one or more occurrences
 * </ul>
 *
 * @param <L> the label type of {@link ExpressionUnit} leaves
 * @param body the sub-expression to quantify
 * @param kind which quantifier to apply
 * @author Florent Guille
 * @since 0.1.0
 */
public record ExpressionQuantifier<L>(Expression<L> body, Kind kind) implements Expression<L> {

  /**
   * The three classic regex quantifiers.
   *
   * @author Florent Guille
   * @since 0.1.0
   */
  public enum Kind {
    /** {@code ?} -- zero or one occurrence. */
    OPTIONAL,
    /** {@code *} -- zero or more occurrences. */
    STAR,
    /** {@code +} -- one or more occurrences. */
    PLUS
  }

  @Override
  public List<Expression<L>> getChildren() {
    return List.of(body);
  }

  @Override
  public String getSignature() {
    return switch (kind) {
      case OPTIONAL -> "Quantifier(?)";
      case STAR -> "Quantifier(*)";
      case PLUS -> "Quantifier(+)";
    };
  }
}
