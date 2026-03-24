package org.kuneiphorm.daedalus.core;

import java.util.List;
import java.util.stream.Stream;

/**
 * A node in a regex-like expression tree.
 *
 * <p>The four concrete types cover the classic regular expression algebra:
 *
 * <ul>
 *   <li>{@link ExpressionChoice} -- alternation ({@code a | b})
 *   <li>{@link ExpressionSequence} -- concatenation ({@code ab})
 *   <li>{@link ExpressionQuantifier} -- repetition ({@code a?}, {@code a*}, {@code a+})
 *   <li>{@link ExpressionUnit} -- atomic leaf carrying an opaque label
 * </ul>
 *
 * <p>The label type {@code L} represents the type of atomic elements matched by {@link
 * ExpressionUnit} leaves. Its interpretation is defined by the consuming module and is opaque to
 * daedalus.
 *
 * <p>Because this is a sealed interface, algorithm code can switch over its subtypes exhaustively:
 *
 * <pre>{@code
 * switch (expr) {
 *   case ExpressionChoice<?>    c -> ...
 *   case ExpressionSequence<?>  s -> ...
 *   case ExpressionQuantifier<?> q -> ...
 *   case ExpressionUnit<?>      u -> ...
 * }
 * }</pre>
 *
 * @param <L> the label type of {@link ExpressionUnit} leaves
 * @author Florent Guille
 * @since 0.1.0
 */
public sealed interface Expression<L>
    permits ExpressionChoice, ExpressionSequence, ExpressionQuantifier, ExpressionUnit {

  /**
   * Returns the direct children of this node. Leaf nodes return an empty list.
   *
   * @return an unmodifiable list of child expressions
   */
  List<Expression<L>> getChildren();

  /**
   * Returns a compact string describing this node only, without its children. Examples: {@code
   * Choice(3)}, {@code Sequence(2)}, {@code Quantifier(*)}, {@code Unit(a)}.
   *
   * @return the signature string
   */
  String getSignature();

  // -------------------------------------------------------------------------
  // Static factories
  // -------------------------------------------------------------------------

  /**
   * Creates a choice (alternation) from the given alternatives.
   *
   * @param <L> the label type
   * @param alternatives the alternative expressions
   * @return a new {@link ExpressionChoice}
   */
  @SafeVarargs
  static <L> ExpressionChoice<L> choice(Expression<L>... alternatives) {
    return new ExpressionChoice<>(List.of(alternatives));
  }

  /**
   * Creates a choice (alternation) from the given list of alternatives.
   *
   * @param <L> the label type
   * @param alternatives the alternative expressions
   * @return a new {@link ExpressionChoice}
   */
  static <L> ExpressionChoice<L> choice(List<Expression<L>> alternatives) {
    return new ExpressionChoice<>(alternatives);
  }

  /**
   * Creates a sequence (concatenation) from the given elements.
   *
   * @param <L> the label type
   * @param elements the elements to concatenate
   * @return a new {@link ExpressionSequence}
   */
  @SafeVarargs
  static <L> ExpressionSequence<L> sequence(Expression<L>... elements) {
    return new ExpressionSequence<>(List.of(elements));
  }

  /**
   * Creates a sequence (concatenation) from the given list of elements.
   *
   * @param <L> the label type
   * @param elements the elements to concatenate
   * @return a new {@link ExpressionSequence}
   */
  static <L> ExpressionSequence<L> sequence(List<Expression<L>> elements) {
    return new ExpressionSequence<>(elements);
  }

  /**
   * Creates an optional quantifier ({@code ?}) over the given body.
   *
   * @param <L> the label type
   * @param body the expression to make optional
   * @return a new {@link ExpressionQuantifier} with kind {@link ExpressionQuantifier.Kind#OPTIONAL}
   */
  static <L> ExpressionQuantifier<L> optional(Expression<L> body) {
    return new ExpressionQuantifier<>(body, ExpressionQuantifier.Kind.OPTIONAL);
  }

  /**
   * Creates a star quantifier ({@code *}) over the given body.
   *
   * @param <L> the label type
   * @param body the expression to repeat zero or more times
   * @return a new {@link ExpressionQuantifier} with kind {@link ExpressionQuantifier.Kind#STAR}
   */
  static <L> ExpressionQuantifier<L> star(Expression<L> body) {
    return new ExpressionQuantifier<>(body, ExpressionQuantifier.Kind.STAR);
  }

  /**
   * Creates a plus quantifier ({@code +}) over the given body.
   *
   * @param <L> the label type
   * @param body the expression to repeat one or more times
   * @return a new {@link ExpressionQuantifier} with kind {@link ExpressionQuantifier.Kind#PLUS}
   */
  static <L> ExpressionQuantifier<L> plus(Expression<L> body) {
    return new ExpressionQuantifier<>(body, ExpressionQuantifier.Kind.PLUS);
  }

  /**
   * Creates a unit (leaf) expression carrying the given label.
   *
   * @param <L> the label type
   * @param label the opaque label for this unit
   * @return a new {@link ExpressionUnit}
   */
  static <L> ExpressionUnit<L> unit(L label) {
    return new ExpressionUnit<>(label);
  }

  // -------------------------------------------------------------------------
  // Algorithms
  // -------------------------------------------------------------------------

  /**
   * Returns a stream of all nodes in this tree in <em>prefix order</em> (pre-order): each node
   * appears before its children, children are visited left-to-right.
   *
   * <p>Example for {@code Choice(Sequence(a, b), c)}: {@code Choice, Sequence, a, b, c}.
   *
   * @param <L> the label type
   * @param expr the root of the tree to unfold
   * @return a stream of all nodes in prefix order
   */
  static <L> Stream<Expression<L>> unfoldPrefix(Expression<L> expr) {
    return Stream.concat(
        Stream.of(expr), expr.getChildren().stream().flatMap(Expression::unfoldPrefix));
  }

  /**
   * Returns a stream of all nodes in this tree in <em>postfix order</em> (post-order): each node
   * appears after its children, children are visited left-to-right.
   *
   * <p>Example for {@code Choice(Sequence(a, b), c)}: {@code a, b, Sequence, c, Choice}.
   *
   * @param <L> the label type
   * @param expr the root of the tree to unfold
   * @return a stream of all nodes in postfix order
   */
  static <L> Stream<Expression<L>> unfoldPostfix(Expression<L> expr) {
    return Stream.concat(
        expr.getChildren().stream().flatMap(Expression::unfoldPostfix), Stream.of(expr));
  }
}
