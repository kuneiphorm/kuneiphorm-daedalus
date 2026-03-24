package org.kuneiphorm.daedalus.range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kuneiphorm.daedalus.core.Pair;

/**
 * A contiguous inclusive range of integers {@code [lo, hi]}.
 *
 * <p>When used in a sorted {@link java.util.List} passed to a {@link Classifier} implementation,
 * the class ID of a range is its index in that list.
 *
 * @param lo the inclusive lower bound
 * @param hi the inclusive upper bound
 * @author Florent Guille
 * @since 0.1.0
 */
public record IntRange(int lo, int hi) implements Comparable<IntRange> {

  /** Validates that {@code lo <= hi}. */
  public IntRange {
    if (lo > hi) throw new IllegalArgumentException("lo (" + lo + ") must be <= hi (" + hi + ")");
  }

  /**
   * Returns {@code true} if {@code c} falls within {@code [lo, hi]}.
   *
   * @param c the integer to test
   * @return whether {@code c} is contained in this range
   */
  public boolean contains(int c) {
    return lo <= c && c <= hi;
  }

  /**
   * Returns {@code true} if this range and {@code other} share at least one integer.
   *
   * @param other the range to test against
   * @return whether the two ranges overlap
   */
  public boolean overlaps(IntRange other) {
    return lo <= other.hi && other.lo <= hi;
  }

  @Override
  public int compareTo(IntRange o) {
    if (lo == o.lo) {
      return Integer.compare(hi, o.hi);
    }
    return Integer.compare(lo, o.lo);
  }

  /**
   * Merges overlapping or adjacent ranges into a sorted, non-overlapping list.
   *
   * @param ranges the ranges to normalize
   * @return a new list of merged, sorted, non-overlapping ranges
   */
  public static List<IntRange> normalize(List<IntRange> ranges) {
    if (ranges.isEmpty()) {
      return List.of();
    }

    if (ranges.size() == 1) {
      return List.of(ranges.get(0));
    }

    List<IntRange> sorted = new ArrayList<>(ranges);
    sorted.sort(null);

    List<IntRange> result = new ArrayList<>();

    Iterator<IntRange> it = sorted.iterator();

    int currentLo = sorted.get(0).lo();
    int currentHi = sorted.get(0).hi();
    it.next();

    while (it.hasNext()) {
      IntRange current = it.next();

      if (current.lo() <= currentHi + 1) {
        currentHi = Math.max(currentHi, current.hi());
      } else {
        result.add(new IntRange(currentLo, currentHi));
        currentLo = current.lo();
        currentHi = current.hi();
      }
    }

    result.add(new IntRange(currentLo, currentHi));

    return result;
  }

  /**
   * Computes the complement of the given sorted, non-overlapping ranges within {@code [lo, hi]}.
   *
   * @param ranges the ranges to negate (must be sorted and non-overlapping)
   * @param lo the inclusive lower bound of the domain
   * @param hi the inclusive upper bound of the domain
   * @return a new list of ranges representing the complement
   */
  public static List<IntRange> negate(List<IntRange> ranges, int lo, int hi) {
    if (ranges.isEmpty()) {
      return List.of(new IntRange(lo, hi));
    }

    List<IntRange> result = new ArrayList<>();

    Iterator<IntRange> it = ranges.iterator();

    IntRange previous = it.next();

    if (previous.lo() > lo) {
      result.add(new IntRange(lo, previous.lo() - 1));
    }

    while (it.hasNext()) {
      IntRange current = it.next();
      result.add(new IntRange(previous.hi() + 1, current.lo() - 1));
      previous = current;
    }

    if (previous.hi() < hi) {
      result.add(new IntRange(previous.hi() + 1, hi));
    }

    return result;
  }

  /**
   * Partitions a list of labeled ranges into non-overlapping sub-ranges, each carrying the union of
   * labels that cover it.
   *
   * <p>The algorithm creates start/end event points for each range, sorts them by index, and sweeps
   * left-to-right with a reference-counted active set. A new sub-range is emitted each time the
   * active label set changes.
   *
   * @param <L> the element type within the label sets
   * @param ranges the labeled ranges to partition, each as a {@link Pair} of {@link IntRange} and
   *     label set
   * @return a list of non-overlapping labeled ranges covering the same domain
   */
  public static <L> List<Pair<IntRange, Set<L>>> partition(List<Pair<IntRange, Set<L>>> ranges) {
    if (ranges.isEmpty()) {
      return List.of();
    }

    if (ranges.size() == 1) {
      return List.of(ranges.get(0));
    }

    record LabeledPoint<L>(int index, boolean isStart, Set<L> labels)
        implements Comparable<LabeledPoint<L>> {

      @Override
      public int compareTo(LabeledPoint<L> o) {
        return Integer.compare(index, o.index);
      }
    }

    List<LabeledPoint<L>> points = new ArrayList<>();
    for (Pair<IntRange, Set<L>> range : ranges) {
      points.add(new LabeledPoint<>(range.first().lo(), true, range.second()));
      points.add(new LabeledPoint<>(range.first().hi() + 1, false, range.second()));
    }

    points.sort(null);

    Map<L, Integer> counts = new HashMap<>();
    int previous = points.get(0).index();

    List<Pair<IntRange, Set<L>>> result = new ArrayList<>();
    for (LabeledPoint<L> point : points) {
      if (point.isStart()) {
        for (L label : point.labels()) {
          int count = counts.getOrDefault(label, 0);

          if (count == 0) {
            if (previous != point.index() && !counts.isEmpty()) {
              result.add(
                  new Pair<>(
                      new IntRange(previous, point.index() - 1), new HashSet<>(counts.keySet())));
            }
            previous = point.index();
            counts.put(label, 1);
          } else {
            counts.put(label, count + 1);
          }
        }
      } else {
        for (L label : point.labels()) {
          int count = counts.get(label);

          if (count == 1) {
            if (previous != point.index()) {
              result.add(
                  new Pair<>(
                      new IntRange(previous, point.index() - 1), new HashSet<>(counts.keySet())));
            }
            previous = point.index();
            counts.remove(label);
          } else {
            counts.put(label, count - 1);
          }
        }
      }
    }

    return result;
  }
}
