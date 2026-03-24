package org.kuneiphorm.daedalus.automaton;

/**
 * A directed edge in an {@link Automaton} from one {@link State} to another.
 *
 * <p>A {@code null} label denotes an epsilon transition (spontaneous, consuming no input).
 *
 * @param <S> the output label type of states
 * @param <L> the transition label type; {@code null} means epsilon
 * @param label the transition label, or {@code null} for an epsilon transition
 * @param target the destination state
 * @author Florent Guille
 * @since 0.1.0
 */
public record Transition<S, L>(L label, State<S, L> target) {

  /**
   * Returns {@code true} if this is an epsilon transition (label is {@code null}).
   *
   * @return whether this transition is an epsilon transition
   */
  public boolean isEpsilon() {
    return label == null;
  }

  /**
   * Returns {@code -label-> target} for labeled transitions and {@code -ε-> target} for epsilon.
   */
  @Override
  public String toString() {
    return (label != null ? "-" + label + "-> " : "-ε-> ") + target;
  }
}
