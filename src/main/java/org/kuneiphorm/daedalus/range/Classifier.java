package org.kuneiphorm.daedalus.range;

/**
 * Maps an integer (typically a character code) to an equivalence class ID.
 *
 * <p>Returns {@code -1} if the input does not belong to any known class.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public interface Classifier {

  /**
   * Returns the equivalence class ID for the given integer, or {@code -1} if none.
   *
   * @param c the integer to classify
   * @return the class ID, or {@code -1}
   */
  int classify(int c);

  /**
   * Returns the total number of equivalence classes (fragments) known to this classifier.
   *
   * <p>Fragment IDs are in the range {@code [0, fragmentCount())}.
   *
   * @return the number of fragments
   */
  int fragmentCount();
}
