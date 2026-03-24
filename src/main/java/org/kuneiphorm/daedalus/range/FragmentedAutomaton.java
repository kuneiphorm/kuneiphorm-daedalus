package org.kuneiphorm.daedalus.range;

import org.kuneiphorm.daedalus.automaton.Automaton;

/**
 * The result of alphabet fragmentation: a DFA whose transition labels are fragment IDs, paired with
 * the {@link Classifier} that maps raw integer inputs to those fragment IDs.
 *
 * @param <S> the output label type for accepting states
 * @param dfa the remapped DFA; transition labels are fragment IDs (non-negative integers)
 * @param classifier the classifier mapping raw input integers to fragment IDs
 * @author Florent Guille
 * @since 0.1.0
 */
public record FragmentedAutomaton<S>(Automaton<S, Integer> dfa, Classifier classifier) {}
