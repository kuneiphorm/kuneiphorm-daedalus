package org.kuneiphorm.daedalus.craft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.automaton.Transition;

/**
 * Computes output reachability for automaton states.
 *
 * <p>For each state, determines the set of output labels reachable by following transitions forward
 * to accepting states. This enables <em>early-accept optimization</em>: if a state can only reach a
 * single output, the automaton can accept at that state without reading further input.
 *
 * <p>The computation uses a fixed-point iteration, propagating output sets forward from accepting
 * states until no set changes.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class OutputRefiner {

  private OutputRefiner() {}

  /**
   * Computes the set of output labels reachable from each state.
   *
   * <p>The result is indexed by state ID. For each state, the set contains all output labels of
   * accepting states reachable via one or more transitions from that state (including the state
   * itself if it is accepting).
   *
   * <ul>
   *   <li>Singleton set -- the state can only reach one output (early-accept candidate).
   *   <li>Empty set -- the state cannot reach any accepting state (dead state).
   *   <li>Multiple elements -- the state is still ambiguous (multiple outputs reachable).
   * </ul>
   *
   * @param <S> the output label type
   * @param <L> the transition label type
   * @param automaton the automaton to analyze
   * @return a list of output sets, indexed by state ID
   */
  public static <S, L> List<Set<S>> computeOutputReachability(Automaton<S, L> automaton) {
    Objects.requireNonNull(automaton, "automaton");
    List<Set<S>> reachableOutputs = new ArrayList<>();

    for (State<S, L> state : automaton.getStates()) {
      Set<S> outputs = new HashSet<>();
      if (state.getOutput() != null) {
        outputs.add(state.getOutput());
      }
      reachableOutputs.add(outputs);
    }

    boolean changed = true;
    while (changed) {
      changed = false;
      for (State<S, L> state : automaton.getStates()) {
        Set<S> outputs = reachableOutputs.get(state.getId());
        for (Transition<S, L> transition : state.getTransitions()) {
          changed |= outputs.addAll(reachableOutputs.get(transition.target().getId()));
        }
      }
    }

    return reachableOutputs;
  }

  /**
   * Refines the given automaton by marking early-accept states and trimming states past the
   * decision point.
   *
   * <p>The algorithm:
   *
   * <ol>
   *   <li>Compute reachable output sets for each state.
   *   <li>Mark states with a singleton reachable set as accepting with that output.
   *   <li>Drop outgoing transitions from singleton states (the decision is made, no further input
   *       needed).
   *   <li>States that become unreachable as a result are excluded from the output.
   * </ol>
   *
   * @param <S> the output label type
   * @param <L> the transition label type
   * @param automaton the DFA to refine; must be deterministic (no epsilon transitions)
   * @return a fresh refined {@link Automaton}
   */
  public static <S, L> Automaton<S, L> refine(Automaton<S, L> automaton) {
    Objects.requireNonNull(automaton, "automaton");
    if (!automaton.isDeterministic()) {
      throw new IllegalArgumentException("Input automaton is not deterministic.");
    }
    List<Set<S>> reachableOutputs = computeOutputReachability(automaton);

    Automaton<S, L> result = Automaton.create();

    // Create states with refined outputs.
    for (State<S, L> state : automaton.getStates()) {
      Set<S> outputs = reachableOutputs.get(state.getId());
      if (outputs.size() == 1) {
        result.newState(outputs.iterator().next());
      } else {
        result.newState(state.getOutput());
      }
    }

    // Copy transitions, skipping outgoing transitions from decided states.
    for (State<S, L> state : automaton.getStates()) {
      if (reachableOutputs.get(state.getId()).size() == 1) {
        continue;
      }
      State<S, L> resultState = result.getStates().get(state.getId());
      for (Transition<S, L> transition : state.getTransitions()) {
        State<S, L> target = result.getStates().get(transition.target().getId());
        resultState.addTransition(transition.label(), target);
      }
    }

    result.setInitialStateId(automaton.getInitial().getId());
    return Trimmer.trim(result);
  }
}
