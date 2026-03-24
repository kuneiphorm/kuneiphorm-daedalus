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
import org.kuneiphorm.daedalus.core.Pair;

/**
 * Converts a non-deterministic finite automaton (NFA) into an equivalent deterministic finite
 * automaton (DFA) using the subset construction (powerset construction) algorithm.
 *
 * <p>The algorithm runs in two phases:
 *
 * <ol>
 *   <li><b>Epsilon closure</b> -- for each NFA state, precomputes the set of all NFA state IDs
 *       reachable via zero or more epsilon transitions.
 *   <li><b>Subset construction</b> -- each DFA state corresponds to a kernel (set of NFA state IDs,
 *       closed under epsilon). Starting from the initial kernel, a worklist processes each kernel:
 *       non-epsilon transitions are handed to a {@link TransitionPartitioner} which groups them
 *       into {@code (label, target set)} pairs. A DFA state is created or reused for each target
 *       kernel, and transitions are added.
 * </ol>
 *
 * <p>The transition partitioning strategy is pluggable via {@link TransitionPartitioner}. The
 * built-in {@link #byEquality()} groups transitions by label equality -- correct when labels are
 * discrete. For overlapping {@link org.kuneiphorm.daedalus.range.IntRange} labels use {@link
 * RangeDeterminizer}, which provides a sweep-line partitioner.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public final class Determinizer {

  private Determinizer() {}

  /**
   * Strategy for partitioning the NFA transitions from a kernel into {@code (label, target set)}
   * pairs, ready for epsilon-closure expansion.
   *
   * <p>The input is a flat list of {@code (label, raw target)} pairs. The output is a list of
   * {@code (label, target set)} pairs where each label maps to the set of raw targets it leads to.
   * The target type {@code T} is opaque to the partitioner -- it serves only as a grouping key.
   *
   * @param <L> the transition label type
   * @param <T> the opaque raw target type (e.g. NFA state ID)
   */
  @FunctionalInterface
  public interface TransitionPartitioner<L, T> {

    /**
     * Partitions the given NFA transitions.
     *
     * @param transitions the flat list of {@code (label, raw target)} pairs from one kernel
     * @return a list of {@code (label, target set)} pairs
     */
    List<Pair<L, Set<T>>> partition(List<Pair<L, T>> transitions);
  }

  /**
   * Picks the DFA output from the non-empty set of candidate outputs (non-null outputs of accepting
   * NFA states in the DFA kernel). Called only when the kernel has at least one accepting state.
   *
   * @param <S> the output label type
   * @param <E> the exception type that may be thrown when resolution fails
   */
  @FunctionalInterface
  public interface OutputResolver<S, E extends Exception> {

    /**
     * Resolves the DFA output from the given set of candidate outputs.
     *
     * @param outputs the non-empty set of candidate outputs
     * @return the selected output
     * @throws E if the output cannot be resolved (e.g. unresolvable conflict)
     */
    S resolve(Set<S> outputs) throws E;
  }

  /**
   * Returns a {@link TransitionPartitioner} that groups transitions by label equality. Transitions
   * with the same label that reach different targets have their target sets merged.
   *
   * <p>Correct when labels are discrete and non-overlapping (e.g. integer class IDs or token
   * types). For overlapping {@link org.kuneiphorm.daedalus.range.IntRange} labels, use {@link
   * RangeDeterminizer#sweepLine} instead.
   *
   * @param <L> the transition label type
   * @param <T> the opaque raw target type
   * @return the equality-based partitioner
   */
  public static <L, T> TransitionPartitioner<L, T> byEquality() {
    return transitions -> {
      Map<L, Set<T>> byLabel = new HashMap<>();
      for (Pair<L, T> t : transitions) {
        byLabel.computeIfAbsent(t.first(), k -> new HashSet<>()).add(t.second());
      }
      List<Pair<L, Set<T>>> result = new ArrayList<>();
      for (Map.Entry<L, Set<T>> entry : byLabel.entrySet()) {
        result.add(new Pair<>(entry.getKey(), entry.getValue()));
      }
      return result;
    };
  }

  /**
   * Determinizes the given NFA using label-equality grouping and first-non-null output resolution.
   *
   * @param <S> the output label type for accepting states
   * @param <L> the transition label type
   * @param nfa the NFA to determinize; must have an initial state set
   * @return a fresh {@link Automaton} representing the equivalent DFA
   */
  public static <S, L> Automaton<S, L> determinize(Automaton<S, L> nfa) {
    Objects.requireNonNull(nfa, "nfa");
    return determinize(nfa, byEquality(), outputs -> outputs.iterator().next());
  }

  /**
   * Determinizes the given NFA using the supplied partitioner and output resolver.
   *
   * @param <S> the output label type for accepting states
   * @param <L> the transition label type
   * @param nfa the NFA to determinize; must have an initial state set
   * @param <E> the exception type that {@code outputResolver} may throw
   * @param partitioner strategy for grouping NFA transitions into {@code (label, target set)} pairs
   * @param outputResolver picks the DFA output from the set of candidate outputs (non-null outputs
   *     of accepting NFA states in the kernel); not called when the kernel has no accepting states
   * @return a fresh {@link Automaton} representing the equivalent DFA
   * @throws E if {@code outputResolver} fails to resolve an output
   */
  public static <S, L, E extends Exception> Automaton<S, L> determinize(
      Automaton<S, L> nfa,
      TransitionPartitioner<L, Integer> partitioner,
      OutputResolver<S, E> outputResolver)
      throws E {
    Objects.requireNonNull(nfa, "nfa");
    Objects.requireNonNull(partitioner, "partitioner");
    Objects.requireNonNull(outputResolver, "outputResolver");

    // Phase 1: precompute epsilon closures for all NFA states.
    HashMap<Integer, Set<Integer>> epsClosure = new HashMap<>();
    for (State<S, L> state : nfa.getStates()) {
      epsClosure.put(state.getId(), computeEpsClosure(state, nfa));
    }

    // Phase 2: subset construction.
    Automaton<S, L> dfa = Automaton.<S, L>create();
    Map<Set<Integer>, State<S, L>> kernelToDfa = new HashMap<>();
    ArrayDeque<Set<Integer>> worklist = new ArrayDeque<>();

    Set<Integer> initialKernel = epsilonClosure(Set.of(nfa.getInitial().getId()), epsClosure);
    State<S, L> initialState = dfa.newState(resolveOutput(initialKernel, nfa, outputResolver));
    dfa.setInitialStateId(initialState.getId());
    kernelToDfa.put(initialKernel, initialState);
    worklist.add(initialKernel);

    while (!worklist.isEmpty()) {
      Set<Integer> kernel = worklist.poll();
      State<S, L> dfaState = kernelToDfa.get(kernel);

      // Collect non-epsilon transitions from all NFA states in this kernel.
      ArrayList<Pair<L, Integer>> transitions = new ArrayList<>();
      for (int stateId : kernel) {
        for (Transition<S, L> t : nfa.getStates().get(stateId).getTransitions()) {
          if (t.label() != null) {
            transitions.add(new Pair<>(t.label(), t.target().getId()));
          }
        }
      }

      // Partition and build DFA transitions.
      for (Pair<L, Set<Integer>> entry : partitioner.partition(transitions)) {
        Set<Integer> targetKernel = epsilonClosure(entry.second(), epsClosure);
        State<S, L> targetDfaState = kernelToDfa.get(targetKernel);
        if (targetDfaState == null) {
          targetDfaState = dfa.newState(resolveOutput(targetKernel, nfa, outputResolver));
          kernelToDfa.put(targetKernel, targetDfaState);
          worklist.add(targetKernel);
        }
        dfaState.addTransition(entry.first(), targetDfaState);
      }
    }

    return dfa;
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static <S, L, E extends Exception> S resolveOutput(
      Set<Integer> kernel, Automaton<S, L> nfa, OutputResolver<S, E> outputResolver) throws E {
    HashSet<S> outputs = new HashSet<>();
    for (int id : kernel) {
      S out = nfa.getStates().get(id).getOutput();
      if (out != null) outputs.add(out);
    }
    return outputs.isEmpty() ? null : outputResolver.resolve(outputs);
  }

  private static <S, L> Set<Integer> computeEpsClosure(State<S, L> start, Automaton<S, L> nfa) {
    HashSet<Integer> closure = new HashSet<>();
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    closure.add(start.getId());
    queue.add(start.getId());
    while (!queue.isEmpty()) {
      Integer id = queue.poll();
      for (Transition<S, L> t : nfa.getStates().get(id).getTransitions()) {
        if (t.label() == null && closure.add(t.target().getId())) {
          queue.add(t.target().getId());
        }
      }
    }
    return closure;
  }

  private static Set<Integer> epsilonClosure(
      Set<Integer> states, Map<Integer, Set<Integer>> epsClosure) {
    HashSet<Integer> closure = new HashSet<>();
    for (int id : states) {
      closure.addAll(epsClosure.get(id));
    }
    return closure;
  }
}
