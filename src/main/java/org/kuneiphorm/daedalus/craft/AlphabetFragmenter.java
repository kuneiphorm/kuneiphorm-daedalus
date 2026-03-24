package org.kuneiphorm.daedalus.craft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.automaton.Transition;
import org.kuneiphorm.daedalus.core.Pair;
import org.kuneiphorm.daedalus.range.BinarySearchClassifier;
import org.kuneiphorm.daedalus.range.FragmentedAutomaton;
import org.kuneiphorm.daedalus.range.IntRange;

/**
 * Fragments the alphabet of an {@link IntRange}-labeled DFA by computing the minimal set of
 * equivalence classes (fragments) over the input ranges and remapping transitions to fragment IDs.
 *
 * <p>Two integers belong to the same fragment if they produce identical transition behavior on
 * every state. The algorithm:
 *
 * <ol>
 *   <li><b>Transition collection</b> — for each state and each of its transitions, a labeled range
 *       is created carrying the {@code (source state ID, target state ID)} pair as its label.
 *   <li><b>Partition</b> — {@link IntRange#partition(List)} normalizes all labeled ranges into
 *       non-overlapping sub-ranges, each carrying the union of {@code (source, target)} pairs that
 *       cover it. Fragment ID = index in the resulting list.
 *   <li><b>DFA remapping</b> — a fresh DFA is built with the same structure but transition labels
 *       replaced by fragment IDs. The classifier is a {@link BinarySearchClassifier} built from the
 *       partitioned ranges.
 * </ol>
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public final class AlphabetFragmenter {

  private AlphabetFragmenter() {}

  /**
   * Fragments the alphabet of the given {@link IntRange}-labeled DFA.
   *
   * @param <S> the output label type for accepting states
   * @param dfa the DFA to fragment; must be deterministic and have an initial state set
   * @return a {@link FragmentedAutomaton} with fragment-ID transitions and a {@link
   *     BinarySearchClassifier}
   */
  public static <S> FragmentedAutomaton<S> fragment(Automaton<S, IntRange> dfa) {
    Objects.requireNonNull(dfa, "dfa");

    // Phase 1: collect labeled ranges — each transition contributes a (source, target) pair.
    List<Pair<IntRange, Set<Pair<Integer, Integer>>>> ranges = new ArrayList<>();
    for (State<S, IntRange> state : dfa.getStates()) {
      for (Transition<S, IntRange> t : state.getTransitions()) {
        Set<Pair<Integer, Integer>> label = new HashSet<>();
        label.add(new Pair<>(state.getId(), t.target().getId()));
        ranges.add(new Pair<>(t.label(), label));
      }
    }

    // Phase 2: partition into non-overlapping sub-ranges. Fragment ID = index.
    List<Pair<IntRange, Set<Pair<Integer, Integer>>>> partitioned = IntRange.partition(ranges);

    // Phase 3: build classifier from partitioned ranges.
    List<IntRange> classifierRanges = new ArrayList<>();
    for (Pair<IntRange, Set<Pair<Integer, Integer>>> interval : partitioned) {
      classifierRanges.add(interval.first());
    }
    BinarySearchClassifier classifier = new BinarySearchClassifier(classifierRanges);

    // Phase 4: build the remapped DFA using fragment IDs.
    Automaton<S, Integer> remapped = Automaton.<S, Integer>create();
    List<State<S, IntRange>> dfaStates = dfa.getStates();
    for (State<S, IntRange> state : dfaStates) {
      remapped.newState(state.getOutput());
    }
    remapped.setInitialStateId(dfa.getInitial().getId());

    List<State<S, Integer>> remappedStates = remapped.getStates();
    for (int fragmentId = 0; fragmentId < partitioned.size(); fragmentId++) {
      Pair<IntRange, Set<Pair<Integer, Integer>>> interval = partitioned.get(fragmentId);
      for (Pair<Integer, Integer> entry : interval.second()) {
        State<S, Integer> source = remappedStates.get(entry.first());
        State<S, Integer> target = remappedStates.get(entry.second());
        source.addTransition(fragmentId, target);
      }
    }

    return new FragmentedAutomaton<>(remapped, classifier);
  }
}
