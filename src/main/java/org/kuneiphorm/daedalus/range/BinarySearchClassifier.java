package org.kuneiphorm.daedalus.range;

import java.util.List;

/**
 * A {@link Classifier} that uses binary search over a sorted list of {@link IntRange}s.
 *
 * <p>The class ID of an integer is the index of the matching range in the list. Returns {@code -1}
 * if no range contains the input.
 *
 * <p>Runs in O(log n) per lookup where n is the number of ranges.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class BinarySearchClassifier implements Classifier {

  private final List<IntRange> ranges;

  /**
   * Constructs a binary search classifier from the given ranges.
   *
   * @param ranges a sorted, non-overlapping list of {@link IntRange}s
   */
  public BinarySearchClassifier(List<IntRange> ranges) {
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
    int lo = 0, hi = ranges.size() - 1;
    while (lo <= hi) {
      int mid = (lo + hi) >>> 1;
      IntRange range = ranges.get(mid);
      if (c < range.lo()) hi = mid - 1;
      else if (c > range.hi()) lo = mid + 1;
      else return mid;
    }
    return -1;
  }
}
