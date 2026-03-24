package org.kuneiphorm.daedalus.craft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Objects;
import org.kuneiphorm.daedalus.automaton.Automaton;
import org.kuneiphorm.daedalus.automaton.State;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.daedalus.core.ExpressionChoice;
import org.kuneiphorm.daedalus.core.ExpressionQuantifier;
import org.kuneiphorm.daedalus.core.ExpressionSequence;
import org.kuneiphorm.daedalus.core.ExpressionUnit;

/**
 * Converts a regex-like {@link Expression} tree into a non-deterministic finite automaton (NFA)
 * using Thompson's construction.
 *
 * <p>The algorithm unfolds the expression in postfix order and maintains two stacks ({@code
 * startStack} and {@code endStack}) tracking the entry and exit states of each NFA fragment. Each
 * expression node pops its children's fragments, wires them together, and pushes the combined
 * fragment. At the end the single remaining exit state receives the output label.
 *
 * <p>The resulting NFA may contain epsilon transitions and is genuinely non-deterministic; it must
 * be determinized (e.g. via {@link Determinizer}) before use.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class ExpressionConverter {

  private ExpressionConverter() {}

  /**
   * Builds an NFA recognizing the language described by {@code expr}, with a single accepting state
   * carrying {@code output}.
   *
   * @param <S> the output label type for the accepting state
   * @param <L> the transition label type; {@code ExpressionUnit} labels must not be {@code null}
   * @param expr the expression tree to compile
   * @param output the output label assigned to the NFA's unique accepting state
   * @return a fresh {@link Automaton} representing the NFA
   */
  public static <S, L> Automaton<S, L> build(Expression<L> expr, S output) {
    Objects.requireNonNull(expr, "expr");
    Automaton<S, L> nfa = Automaton.<S, L>create();
    Deque<State<S, L>> startStack = new ArrayDeque<>();
    Deque<State<S, L>> endStack = new ArrayDeque<>();

    for (Expression<L> node : Expression.unfoldPostfix(expr).toList()) {
      switch (node) {
        case ExpressionUnit<L> u -> {
          State<S, L> start = nfa.newState();
          State<S, L> end = nfa.newState();
          start.addTransition(u.label(), end);
          startStack.push(start);
          endStack.push(end);
        }

        case ExpressionSequence<L> s -> {
          int n = s.elements().size();
          ArrayList<State<S, L>> childStarts = new ArrayList<>(n);
          ArrayList<State<S, L>> childEnds = new ArrayList<>(n);
          for (int i = 0; i < n; i++) {
            childStarts.add(startStack.pop());
            childEnds.add(endStack.pop());
          }
          // Stack gives children in reverse definition order; restore left-to-right.
          Collections.reverse(childStarts);
          Collections.reverse(childEnds);

          State<S, L> start = nfa.newState();
          State<S, L> end = start;
          for (int i = 0; i < n; i++) {
            end.addEpsilonTransition(childStarts.get(i));
            end = childEnds.get(i);
          }
          startStack.push(start);
          endStack.push(end);
        }

        case ExpressionChoice<L> c -> {
          int n = c.alternatives().size();
          ArrayList<State<S, L>> childStarts = new ArrayList<>(n);
          ArrayList<State<S, L>> childEnds = new ArrayList<>(n);
          for (int i = 0; i < n; i++) {
            childStarts.add(startStack.pop());
            childEnds.add(endStack.pop());
          }

          State<S, L> start = nfa.newState();
          State<S, L> end = nfa.newState();
          for (int i = 0; i < n; i++) {
            start.addEpsilonTransition(childStarts.get(i));
            childEnds.get(i).addEpsilonTransition(end);
          }
          startStack.push(start);
          endStack.push(end);
        }

        case ExpressionQuantifier<L> q -> {
          State<S, L> childStart = startStack.pop();
          State<S, L> childEnd = endStack.pop();
          ExpressionQuantifier.Kind kind = q.kind();
          if (kind == ExpressionQuantifier.Kind.OPTIONAL) {
            childStart.addEpsilonTransition(childEnd);
          } else if (kind == ExpressionQuantifier.Kind.STAR) {
            childStart.addEpsilonTransition(childEnd);
            childEnd.addEpsilonTransition(childStart);
          } else {
            childEnd.addEpsilonTransition(childStart);
          }
          startStack.push(childStart);
          endStack.push(childEnd);
        }
      }
    }

    nfa.setInitialStateId(startStack.pop().getId());
    endStack.pop().setOutput(output);
    return nfa;
  }
}
