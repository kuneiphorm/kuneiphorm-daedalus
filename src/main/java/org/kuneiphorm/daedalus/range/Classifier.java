package org.kuneiphorm.daedalus.range;

/**
 * Maps an integer (typically a character code) to an equivalence class ID.
 *
 * <p>Returns {@code -1} if the input does not belong to any known class.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
@FunctionalInterface
public interface Classifier {

  /**
   * Returns the equivalence class ID for the given integer, or {@code -1} if none.
   *
   * @param c the integer to classify
   * @return the class ID, or {@code -1}
   */
  int classify(int c);
}
