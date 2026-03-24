package org.kuneiphorm.daedalus.craft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.automaton.Transition;

/**
 * Removes unreachable and useless states from a DFA, producing a <em>trim</em> automaton.
 *
 * <p>A state is <em>reachable</em> if it can be reached from the initial state via some sequence of
 * transitions. A state is <em>co-reachable</em> (useful) if some accepting state can be reached
 * from it. A DFA is trim when every state is both reachable and co-reachable.
 *
 * <p>The algorithm runs in three phases:
 *
 * <ol>
 *   <li><b>Forward BFS</b> from the initial state -- collects all reachable states.
 *   <li><b>Backward BFS</b> from accepting states over reversed edges (restricted to reachable
 *       states) -- collects all co-reachable states.
 *   <li><b>Reconstruction</b> -- builds a fresh automaton containing only states in the
 *       intersection of the two sets. The initial state is always included even when it is not
 *       co-reachable, so the result always represents a valid (possibly empty-language) automaton.
 * </ol>
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class Trimmer {

  private Trimmer() {}

  /**
   * Trims the given DFA, returning an equivalent DFA with no unreachable or useless states.
   *
   * @param <S> the output label type for accepting states
   * @param <L> the transition label type
   * @param dfa the DFA to trim; must have an initial state set
   * @return a fresh trimmed {@link Automaton}
   */
  public static <S, L> Automaton<S, L> trim(Automaton<S, L> dfa) {
    Objects.requireNonNull(dfa, "dfa");
    if (!dfa.isDeterministic()) {
      throw new IllegalArgumentException("Input automaton is not deterministic.");
    }
    State<S, L> initial = dfa.getInitial();

    // Phase 1: forward BFS -- collect reachable states.
    Set<State<S, L>> reachable = new HashSet<>();
    ArrayDeque<State<S, L>> queue = new ArrayDeque<>();
    reachable.add(initial);
    queue.add(initial);
    while (!queue.isEmpty()) {
      State<S, L> state = queue.poll();
      for (Transition<S, L> t : state.getTransitions()) {
        if (reachable.add(t.target())) {
          queue.add(t.target());
        }
      }
    }

    // Phase 2: backward BFS -- collect co-reachable states.
    // Build a reverse adjacency map restricted to reachable states.
    Map<State<S, L>, List<State<S, L>>> reverse = new HashMap<>();
    for (State<S, L> state : reachable) {
      reverse.putIfAbsent(state, new ArrayList<>());
      for (Transition<S, L> t : state.getTransitions()) {
        reverse.computeIfAbsent(t.target(), k -> new ArrayList<>()).add(state);
      }
    }

    Set<State<S, L>> coReachable = new HashSet<>();
    for (State<S, L> state : reachable) {
      if (state.isAccepting()) {
        coReachable.add(state);
        queue.add(state);
      }
    }
    while (!queue.isEmpty()) {
      State<S, L> state = queue.poll();
      for (State<S, L> pred : reverse.getOrDefault(state, List.of())) {
        if (coReachable.add(pred)) {
          queue.add(pred);
        }
      }
    }

    // Phase 3: trim = reachable ∩ co-reachable; initial always included.
    Set<State<S, L>> trim = new HashSet<>(reachable);
    trim.retainAll(coReachable);
    trim.add(initial);

    // Reconstruct the automaton with only trim states, preserving IDs implicitly via a map.
    Automaton<S, L> result = Automaton.<S, L>create();
    Map<State<S, L>, State<S, L>> stateMap = new HashMap<>();
    for (State<S, L> state : dfa.getStates()) {
      if (trim.contains(state)) {
        stateMap.put(state, result.newState(state.getOutput()));
      }
    }

    for (State<S, L> state : dfa.getStates()) {
      if (!trim.contains(state)) continue;
      State<S, L> resultState = stateMap.get(state);
      for (Transition<S, L> t : state.getTransitions()) {
        State<S, L> mappedTarget = stateMap.get(t.target());
        if (mappedTarget != null) {
          resultState.addTransition(t.label(), mappedTarget);
        }
      }
    }

    result.setInitialStateId(stateMap.get(initial).getId());
    return result;
  }
}
