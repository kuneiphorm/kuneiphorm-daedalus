package org.kuneiphorm.daedalus.core;

/**
 * A generic immutable 2-tuple.
 *
 * @param first the first element
 * @param second the second element
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @author Florent Guille
 * @since 0.1.0
 */
public record Pair<A, B>(A first, B second) {}
