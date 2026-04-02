package org.kuneiphorm.daedalus.craft;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.automaton.Transition;

/**
 * Reverses an automaton from a given state.
 *
 * <p>The result is a new automaton where:
 *
 * <ul>
 *   <li>All transitions are flipped (source and target swapped).
 *   <li>The specified state becomes the initial state (non-accepting).
 *   <li>The original initial state becomes the sole accepting state with the given output.
 *   <li>All other outputs are cleared.
 * </ul>
 *
 * <p>The output type of the reversed automaton can differ from the original.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public final class Reverser {

  private Reverser() {}

  /**
   * Reverses the given automaton from the specified state.
   *
   * @param <S> the output label type of the original automaton (ignored in the result)
   * @param <T> the output label type of the reversed automaton
   * @param <L> the transition label type
   * @param automaton the automaton to reverse; must have an initial state set
   * @param startState the state that becomes the initial state of the reversed automaton
   * @param output the output label for the new accepting state (the original initial state)
   * @return a fresh reversed {@link Automaton}
   */
  public static <S, T, L> Automaton<T, L> reverse(
      Automaton<S, L> automaton, State<S, L> startState, T output) {
    Objects.requireNonNull(automaton, "automaton");
    Objects.requireNonNull(startState, "startState");
    Objects.requireNonNull(output, "output");

    int originalInitialId = automaton.getInitial().getId();

    // Create new states: same count, same order.
    Automaton<T, L> result = Automaton.create();
    Map<Integer, State<T, L>> stateMap = new HashMap<>();
    for (int i = 0; i < automaton.stateCount(); i++) {
      State<T, L> newState;
      if (i == originalInitialId) {
        newState = result.newState(output);
      } else {
        newState = result.newState();
      }
      stateMap.put(i, newState);
    }

    // Flip all transitions.
    for (State<S, L> state : automaton.getStates()) {
      for (Transition<S, L> t : state.getTransitions()) {
        State<T, L> newSource = stateMap.get(t.target().getId());
        State<T, L> newTarget = stateMap.get(state.getId());
        newSource.addTransition(t.label(), newTarget);
      }
    }

    result.setInitialStateId(stateMap.get(startState.getId()).getId());
    return result;
  }
}
