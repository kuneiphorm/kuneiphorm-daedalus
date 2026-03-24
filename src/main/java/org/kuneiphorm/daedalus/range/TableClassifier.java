package org.kuneiphorm.daedalus.range;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link Classifier} backed by a flat integer lookup table.
 *
 * <p>The class ID of an integer is the index of the matching range in the original sorted list.
 * Returns {@code -1} for inputs outside the table or not covered by any range.
 *
 * <p>Runs in O(1) per lookup. Only suitable for bounded alphabets where the maximum character code
 * is known and small (e.g. ASCII: 256 entries = 1 KB).
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class TableClassifier implements Classifier {

  private final int[] table;
  private final int fragmentCount;

  /**
   * Constructs a table classifier covering character codes {@code 0} to {@code tableSize - 1}.
   * Codes not covered by any range map to {@code -1}.
   *
   * @param ranges a sorted, non-overlapping list of {@link IntRange}s; all {@code hi} values must
   *     be within {@code [0, tableSize - 1]}
   * @param tableSize the number of entries in the lookup table
   * @throws IllegalArgumentException if any range boundary exceeds {@code tableSize - 1}
   */
  public TableClassifier(List<IntRange> ranges, int tableSize) {
    this.table = new int[tableSize];
    this.fragmentCount = ranges.size();
    Arrays.fill(this.table, -1);
    for (int i = 0; i < ranges.size(); i++) {
      IntRange range = ranges.get(i);
      if (range.hi() >= tableSize) {
        throw new IllegalArgumentException(
            "Range hi (" + range.hi() + ") exceeds tableSize - 1 (" + (tableSize - 1) + ")");
      }
      for (int c = range.lo(); c <= range.hi(); c++) {
        table[c] = i;
      }
    }
  }

  @Override
  public int classify(int c) {
    if (c < 0 || c >= table.length) return -1;
    return table[c];
  }

  @Override
  public int fragmentCount() {
    return fragmentCount;
  }
}
