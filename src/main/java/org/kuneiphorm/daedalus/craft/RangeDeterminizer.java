package org.kuneiphorm.daedalus.craft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.core.Pair;
import org.kuneiphorm.daedalus.range.IntRange;

/**
 * Converts an {@link IntRange}-labeled NFA into an equivalent DFA using a range-aware subset
 * construction.
 *
 * <p>Delegates to {@link Determinizer#determinize(Automaton, Determinizer.TransitionPartitioner,
 * Determinizer.OutputResolver)} with:
 *
 * <ul>
 *   <li>A sweep-line {@link Determinizer.TransitionPartitioner} ({@link #sweepLine}) that splits
 *       overlapping {@link IntRange} transitions at their boundaries via {@link
 *       IntRange#partition(List)}, so that each resulting sub-interval maps to a constant set of
 *       NFA target states.
 *   <li>A priority-based output resolver that picks the highest-priority output label when multiple
 *       accepting NFA states land in the same DFA kernel, using the supplied {@code labelPriority}
 *       map (output label -> priority index; lower index = higher priority).
 * </ul>
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public final class RangeDeterminizer {

  private RangeDeterminizer() {}

  /**
   * Determinizes the given {@link IntRange}-labeled NFA.
   *
   * @param <S> the output label type for accepting states
   * @param nfa the NFA to determinize; must have an initial state set
   * @param labelPriority a map from output label to priority index (lower index = higher priority),
   *     used to resolve output conflicts when multiple accepting states appear in the same kernel;
   *     pass an empty map when priority is irrelevant
   * @return a fresh {@link Automaton} representing the equivalent DFA
   */
  public static <S> Automaton<S, IntRange> determinize(
      Automaton<S, IntRange> nfa, Map<S, Integer> labelPriority) {
    Objects.requireNonNull(nfa, "nfa");
    Objects.requireNonNull(labelPriority, "labelPriority");
    return Determinizer.determinize(
        nfa,
        RangeDeterminizer::sweepLine,
        outputs ->
            outputs.stream()
                .min(Comparator.comparingInt(o -> labelPriority.getOrDefault(o, Integer.MAX_VALUE)))
                .orElseThrow());
  }

  // -------------------------------------------------------------------------
  // Sweep-line partitioning
  // -------------------------------------------------------------------------

  /**
   * Given a list of {@code (IntRange, NFA target state ID)} pairs, partitions the integer line at
   * all range boundaries and returns the resulting sub-intervals paired with their target sets.
   *
   * <p>Delegates to {@link IntRange#partition(List)} for the sweep-line partitioning.
   *
   * @param transitions the flat list of {@code (label, raw NFA target ID)} pairs
   * @return a list of {@code (IntRange, target set)} pairs
   */
  static List<Pair<IntRange, Set<Integer>>> sweepLine(List<Pair<IntRange, Integer>> transitions) {
    if (transitions.isEmpty()) return List.of();

    List<Pair<IntRange, Set<Integer>>> labeled = new ArrayList<>();
    for (Pair<IntRange, Integer> transition : transitions) {
      Set<Integer> targetSet = new HashSet<>();
      targetSet.add(transition.second());
      labeled.add(new Pair<>(transition.first(), targetSet));
    }

    return IntRange.partition(labeled);
  }
}
