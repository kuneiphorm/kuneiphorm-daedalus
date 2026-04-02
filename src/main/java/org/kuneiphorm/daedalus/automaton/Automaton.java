package org.kuneiphorm.daedalus.automaton;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A finite automaton with generic state output labels and transition labels.
 *
 * <p>States are owned by the automaton and must be created through {@link #newState()} or {@link
 * #newState(Object)}. Each created state receives a unique integer ID, starting at {@code 0} and
 * incrementing by one for each subsequent state.
 *
 * <p>The initial state is not set at construction time. It must be designated explicitly via {@link
 * #setInitialStateId(int)} before calling {@link #getInitial()}. This avoids silently pre-creating
 * a state that algorithms may not intend to use as the entry point.
 *
 * <p>Whether the automaton is deterministic (DFA) or non-deterministic (NFA) is a property of the
 * transitions added to its states -- epsilon transitions ({@code null} label) and multiple
 * transitions on the same label make it an NFA.
 *
 * @param <S> the output label type for accepting states; {@code null} = non-accepting
 * @param <L> the transition label type; {@code null} = epsilon
 * @author Florent Guille
 * @since 0.1.0
 */
public class Automaton<S, L> {

  private final List<State<S, L>> states = new ArrayList<>();
  private int initialStateId = -1;

  private Automaton() {}

  /**
   * Creates a new empty automaton with no states and no initial state designated.
   *
   * @param <S> the output label type for accepting states
   * @param <L> the transition label type
   * @return a new empty automaton
   */
  public static <S, L> Automaton<S, L> create() {
    return new Automaton<>();
  }

  /**
   * Returns the initial state.
   *
   * @return the initial state
   * @throws IllegalStateException if no initial state has been designated via {@link
   *     #setInitialStateId(int)}
   */
  public State<S, L> getInitial() {
    if (initialStateId == -1) {
      throw new IllegalStateException("No initial state has been set.");
    }
    return states.get(initialStateId);
  }

  /**
   * Designates the state with the given ID as the initial state.
   *
   * @param id the ID of an existing state
   * @throws IllegalArgumentException if {@code id} does not correspond to an existing state
   */
  public void setInitialStateId(int id) {
    if (id < 0 || id >= states.size()) {
      throw new IllegalArgumentException("No state with ID " + id + " exists.");
    }
    this.initialStateId = id;
  }

  /**
   * Returns an unmodifiable view of all states owned by this automaton, in creation order.
   *
   * @return the list of states
   */
  public List<State<S, L>> getStates() {
    return Collections.unmodifiableList(states);
  }

  /**
   * Creates and registers a new non-accepting state.
   *
   * @return the newly created state
   */
  public State<S, L> newState() {
    State<S, L> state = new State<>(states.size());
    states.add(state);
    return state;
  }

  /**
   * Creates and registers a new accepting state with the given output label.
   *
   * @param output the output label for the accepting state
   * @return the newly created state
   */
  public State<S, L> newState(S output) {
    State<S, L> state = new State<>(states.size(), output);
    states.add(state);
    return state;
  }

  /**
   * Returns the number of states in this automaton.
   *
   * @return the state count
   */
  public int stateCount() {
    return states.size();
  }

  /**
   * Returns an unmodifiable list of all accepting states (states whose output is non-null).
   *
   * @return the list of accepting states, in creation order
   */
  public List<State<S, L>> getAcceptingStates() {
    return states.stream().filter(State::isAccepting).collect(Collectors.toUnmodifiableList());
  }

  /**
   * Returns {@code true} if this automaton is deterministic: no epsilon transitions and no state
   * has two transitions with the same label.
   *
   * @return whether this automaton is a DFA
   */
  public boolean isDeterministic() {
    for (State<S, L> state : states) {
      List<Transition<S, L>> transitions = state.getTransitions();
      for (int i = 0; i < transitions.size(); i++) {
        Transition<S, L> ti = transitions.get(i);
        if (ti.isEpsilon()) {
          return false;
        }
        for (int j = i + 1; j < transitions.size(); j++) {
          if (ti.label().equals(transitions.get(j).label())) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if the accepted language is empty -- i.e., no accepting state is reachable
   * from the initial state.
   *
   * @return whether the automaton accepts no strings
   */
  public boolean isEmpty() {
    Set<Integer> visited = new HashSet<>();
    Deque<Integer> worklist = new ArrayDeque<>();
    visited.add(getInitial().getId());
    worklist.add(getInitial().getId());

    while (!worklist.isEmpty()) {
      int stateId = worklist.poll();
      State<S, L> state = states.get(stateId);
      if (state.isAccepting()) {
        return false;
      }
      for (Transition<S, L> transition : state.getTransitions()) {
        if (visited.add(transition.target().getId())) {
          worklist.add(transition.target().getId());
        }
      }
    }

    return true;
  }

  /**
   * Returns a new automaton with the same states and transitions but with output labels transformed
   * by the given function. A {@code null} output (non-accepting state) is not passed to the
   * function and remains {@code null}.
   *
   * @param <T> the new output label type
   * @param mapper the function to apply to each non-null output
   * @return a fresh {@link Automaton} with mapped outputs
   */
  public <T> Automaton<T, L> mapOutputs(Function<S, T> mapper) {
    Automaton<T, L> result = Automaton.create();

    for (State<S, L> state : states) {
      if (state.isAccepting()) {
        result.newState(mapper.apply(state.getOutput()));
      } else {
        result.newState();
      }
    }

    for (State<S, L> state : states) {
      State<T, L> resultState = result.getStates().get(state.getId());
      for (Transition<S, L> transition : state.getTransitions()) {
        State<T, L> target = result.getStates().get(transition.target().getId());
        resultState.addTransition(transition.label(), target);
      }
    }

    result.setInitialStateId(getInitial().getId());
    return result;
  }

  /**
   * Returns a multiline representation listing all states and their transitions.
   *
   * <p>Example:
   *
   * <pre>
   * Automaton[3 states]
   *   q0 (initial)
   *     -a-> q1
   *     -ε-> q2
   *   q1[TOKEN]
   *   q2
   *     -b-> q1
   * </pre>
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Automaton[")
        .append(states.size())
        .append(" state")
        .append(states.size() == 1 ? "" : "s")
        .append("]\n");
    for (State<S, L> state : states) {
      sb.append("  ").append(state);
      if (state.getId() == initialStateId) sb.append(" (initial)");
      sb.append("\n");
      for (Transition<S, L> t : state.getTransitions()) {
        sb.append("    ").append(t).append("\n");
      }
    }
    return sb.toString().stripTrailing();
  }
}
