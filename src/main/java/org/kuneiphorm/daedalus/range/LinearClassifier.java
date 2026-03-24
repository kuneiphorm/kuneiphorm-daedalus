package org.kuneiphorm.daedalus.range;

import java.util.List;

/**
 * A {@link Classifier} that performs a linear scan over a sorted list of {@link IntRange}s.
 *
 * <p>The class ID of an integer is the index of the matching range in the list. Returns {@code -1}
 * if no range contains the input.
 *
 * <p>Runs in O(n) per lookup where n is the number of ranges.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class LinearClassifier implements Classifier {

  private final List<IntRange> ranges;

  /**
   * Constructs a linear classifier from the given ranges.
   *
   * @param ranges a sorted, non-overlapping list of {@link IntRange}s
   */
  public LinearClassifier(List<IntRange> ranges) {
    this.ranges = List.copyOf(ranges);
  }

  /**
   * Returns the ranges backing this classifier, in sorted order.
   *
   * @return an unmodifiable list of ranges
   */
  public List<IntRange> getRanges() {
    return ranges;
  }

  @Override
  public int classify(int c) {
    for (int i = 0; i < ranges.size(); i++) {
      IntRange range = ranges.get(i);
      if (c < range.lo()) return -1;
      if (c <= range.hi()) return i;
    }
    return -1;
  }
}
