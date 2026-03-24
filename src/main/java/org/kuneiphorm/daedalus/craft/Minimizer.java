package org.kuneiphorm.daedalus.craft;

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
 * Minimizes a deterministic finite automaton (DFA) using a partition-refinement algorithm.
 *
 * <p>The algorithm runs in three phases:
 *
 * <ol>
 *   <li><b>Initial partition</b> -- states are grouped by their output label ({@code null} for
 *       non-accepting states). States with different outputs are trivially distinguishable and must
 *       belong to separate blocks from the start.
 *   <li><b>Refinement</b> -- blocks are repeatedly split until stable. A block is split when two of
 *       its states disagree on a transition label: they either transition to states in different
 *       blocks, or one transitions and the other does not. Splitting continues until a full pass
 *       produces no change (fixed point).
 *   <li><b>Result construction</b> -- each surviving block becomes one state in the minimized DFA.
 *       Any representative state from the block provides the transitions (all states in a block
 *       agree after refinement). The block containing the original initial state becomes the new
 *       initial state.
 * </ol>
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class Minimizer {

  private Minimizer() {}

  /**
   * Minimizes the given DFA, returning an equivalent DFA with the fewest possible states.
   *
   * <p><b>Precondition -- consistent dead-state representation.</b> The input must represent
   * rejection uniformly: either by always omitting transitions that would lead to a dead
   * (rejecting, sink) state (<em>incomplete/partial DFA</em>), or by always including an explicit
   * dead state with transitions to itself (<em>complete DFA</em>). Mixing the two -- where some
   * states have an explicit transition to a dead state on label {@code x} while others simply omit
   * the transition on {@code x} -- causes partition refinement to treat the two representations as
   * distinguishable and produces a non-minimal result. The output of {@link Determinizer} satisfies
   * this precondition naturally, as it never introduces explicit dead-state transitions.
   *
   * <p><b>Note on unreachable states.</b> Partition refinement operates on all states in the
   * automaton regardless of reachability. Unreachable states are preserved in the output (they may
   * be merged with reachable equivalents, but never dropped). Run {@link Trimmer} before or after
   * minimization to remove them.
   *
   * @param <S> the output label type for accepting states
   * @param <L> the transition label type
   * @param dfa the DFA to minimize; must be deterministic and have an initial state set
   * @return a fresh minimized {@link Automaton}
   */
  public static <S, L> Automaton<S, L> minimize(Automaton<S, L> dfa) {
    Objects.requireNonNull(dfa, "dfa");
    // Phase 1: initial partition -- group states by output label.
    // We use a map keyed by output (null for non-accepting) to collect each group.
    Map<S, List<State<S, L>>> byOutput = new HashMap<>();
    for (State<S, L> state : dfa.getStates()) {
      byOutput.computeIfAbsent(state.getOutput(), k -> new ArrayList<>()).add(state);
    }
    List<Set<State<S, L>>> blocks = new ArrayList<>();
    for (List<State<S, L>> group : byOutput.values()) {
      blocks.add(new HashSet<>(group));
    }

    // stateToBlock(state) returns the index in `blocks` of the block containing `state`.
    // We rebuild this map after every split.
    Map<State<S, L>, Integer> stateToBlock = buildStateToBlock(blocks);

    // Phase 2: refinement -- split blocks until stable.
    boolean changed = true;
    while (changed) {
      changed = false;
      List<Set<State<S, L>>> nextBlocks = new ArrayList<>();

      for (Set<State<S, L>> block : blocks) {
        // Attempt to split this block by partitioning its states according to their
        // transition signature: for each label, which block does the target belong to?
        // States with the same signature stay together; different signatures → split.
        Map<Map<L, Integer>, Set<State<S, L>>> bySig = new HashMap<>();
        for (State<S, L> state : block) {
          HashMap<L, Integer> sig = new HashMap<>();
          for (Transition<S, L> t : state.getTransitions()) {
            sig.put(t.label(), stateToBlock.get(t.target()));
          }
          bySig.computeIfAbsent(sig, k -> new HashSet<>()).add(state);
        }

        if (bySig.size() > 1) changed = true;
        nextBlocks.addAll(bySig.values());
      }

      blocks = nextBlocks;
      stateToBlock = buildStateToBlock(blocks);
    }

    // Phase 3: build the minimized DFA.
    // One new state per block; representative = any element of the block.
    Automaton<S, L> minDfa = Automaton.<S, L>create();
    List<State<S, L>> blockStates = new ArrayList<>(blocks.size());

    for (Set<State<S, L>> block : blocks) {
      State<S, L> rep = block.iterator().next();
      blockStates.add(minDfa.newState(rep.getOutput()));
    }

    for (int i = 0; i < blocks.size(); i++) {
      State<S, L> rep = blocks.get(i).iterator().next();
      State<S, L> minState = blockStates.get(i);
      for (Transition<S, L> t : rep.getTransitions()) {
        Integer targetBlock = stateToBlock.get(t.target());
        minState.addTransition(t.label(), blockStates.get(targetBlock));
      }
    }

    // The block containing the original initial state becomes the new initial state.
    Integer initialBlock = stateToBlock.get(dfa.getInitial());
    minDfa.setInitialStateId(blockStates.get(initialBlock).getId());

    return minDfa;
  }

  private static <S, L> Map<State<S, L>, Integer> buildStateToBlock(List<Set<State<S, L>>> blocks) {
    Map<State<S, L>, Integer> map = new HashMap<>();
    for (int i = 0; i < blocks.size(); i++) {
      for (State<S, L> state : blocks.get(i)) {
        map.put(state, i);
      }
    }
    return map;
  }
}
