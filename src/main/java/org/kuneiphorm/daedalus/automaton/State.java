package org.kuneiphorm.daedalus.automaton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A state in an {@link Automaton}.
 *
 * <p>States are created exclusively through {@link Automaton#newState()} and {@link
 * Automaton#newState(Object)}, ensuring every state belongs to an automaton. Each state receives a
 * unique integer ID assigned sequentially at creation time, starting at {@code 0}. The initial
 * state is designated explicitly via {@link Automaton#setInitialStateId(int)} and need not be the
 * first state created.
 *
 * <p>An accepting state carries a non-null output label of type {@code S}. A non-accepting state
 * has a {@code null} output.
 *
 * @param <S> the output label type; {@code null} means non-accepting
 * @param <L> the transition label type
 * @author Florent Guille
 * @since 0.1.0
 */
public class State<S, L> {

  private final int id;
  private S output;
  private final List<Transition<S, L>> transitions = new ArrayList<>();

  State(int id) {
    this.id = id;
    this.output = null;
  }

  State(int id, S output) {
    this.id = id;
    this.output = output;
  }

  /**
   * Returns the unique integer ID of this state within its automaton.
   *
   * @return the state ID
   */
  public int getId() {
    return id;
  }

  /**
   * Returns {@code true} if this state has a non-null output label.
   *
   * @return whether this state is accepting
   */
  public boolean isAccepting() {
    return output != null;
  }

  /**
   * Returns the output label of this state, or {@code null} if non-accepting.
   *
   * @return the output label, or {@code null}
   */
  public S getOutput() {
    return output;
  }

  /**
   * Sets the output label, making this state accepting.
   *
   * @param output the output label
   */
  public void setOutput(S output) {
    this.output = output;
  }

  /** Clears the output label, making this state non-accepting. */
  public void clearOutput() {
    this.output = null;
  }

  /**
   * Returns an unmodifiable view of the outgoing transitions.
   *
   * @return the list of transitions
   */
  public List<Transition<S, L>> getTransitions() {
    return Collections.unmodifiableList(transitions);
  }

  /**
   * Adds a labeled transition to {@code target}.
   *
   * @param label the transition label
   * @param target the target state
   */
  public void addTransition(L label, State<S, L> target) {
    transitions.add(new Transition<>(label, target));
  }

  /**
   * Adds an epsilon transition to {@code target}.
   *
   * @param target the target state
   */
  public void addEpsilonTransition(State<S, L> target) {
    transitions.add(new Transition<>(null, target));
  }

  /**
   * Returns {@code q<id>} for non-accepting states and {@code q<id>[output]} for accepting ones.
   */
  @Override
  public String toString() {
    return output != null ? "q" + id + "[" + output + "]" : "q" + id;
  }
}
