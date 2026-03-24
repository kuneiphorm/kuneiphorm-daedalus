[![CI](https://img.shields.io/github/actions/workflow/status/kuneiphorm/kuneiphorm-daedalus/ci.yml?branch=master&label=CI)](https://github.com/kuneiphorm/kuneiphorm-daedalus/actions)
[![kuneiphorm-daedalus](https://img.shields.io/github/v/release/kuneiphorm/kuneiphorm-daedalus?include_prereleases&label=kuneiphorm-daedalus)](https://github.com/kuneiphorm/kuneiphorm-daedalus/releases)
![License](https://img.shields.io/badge/License-Apache_2.0-blue)
![Java](https://img.shields.io/badge/Java-21-blue)

# kuneiphorm-daedalus

Expression algebra, finite automata, and automaton algorithms for the kuneiphorm toolkit.

This module defines the shared formalism used by `kuneiphorm-regex` and future modules. It provides data structures for expression trees and automata, along with the standard algorithms (Thompson's construction, subset construction, minimization, trimming, alphabet fragmentation). It has **zero external dependencies**.

## Package overview

```
org.kuneiphorm.daedalus
├── core        Expression tree algebra + generic Pair
├── automaton   NFA/DFA container model
├── craft       Construction and transformation algorithms
└── range       Integer range classifiers + fragmented automaton
```

## Packages

### `core` : Expression tree algebra

| Type | Description |
|---|---|
| `Expression<L>` | Sealed interface. Static factories: `unit`, `choice`, `sequence`, `optional`, `star`, `plus`. Traversal: `unfoldPrefix`, `unfoldPostfix`. |
| `ExpressionUnit<L>` | Leaf node carrying an opaque label `L`. |
| `ExpressionChoice<L>` | Alternation (`a \| b`). Empty alternatives = empty language. |
| `ExpressionSequence<L>` | Concatenation (`a b`). Empty elements = epsilon. |
| `ExpressionQuantifier<L>` | Repetition: `OPTIONAL` (`?`), `STAR` (`*`), `PLUS` (`+`). |
| `Pair<A, B>` | Generic immutable 2-tuple. Replaces `Map.Entry` throughout the module. |

The label type `L` is opaque to daedalus. Its meaning is defined by the consuming module (e.g., character ranges in `kuneiphorm-regex`).

### `automaton` : NFA/DFA container model

| Type | Description |
|---|---|
| `Automaton<S, L>` | Generic automaton (NFA or DFA). States created via `newState()`; initial state designated via `setInitialStateId(int)`. Sequential integer IDs. |
| `State<S, L>` | Node with an integer ID, an optional output value `S`, and a list of outgoing transitions. Identity-based equality. |
| `Transition<S, L>` | Record `(L label, State<S, L> target)`. A `null` label denotes an epsilon-transition. |

### `craft` : Construction and transformation algorithms

| Class | Description |
|---|---|
| `ExpressionConverter` | Thompson's construction: `Expression<L>` -> NFA. Uses postfix unfolding and two stacks. |
| `Determinizer` | Subset construction: NFA -> DFA. Pluggable `TransitionPartitioner` and `OutputResolver`. Built-in `byEquality()` for discrete labels. |
| `RangeDeterminizer` | Range-aware determinization for `IntRange`-labeled NFAs. Delegates to `IntRange.partition` (sweep-line) and uses priority-based output resolution. |
| `Minimizer` | Partition refinement: DFA -> minimized DFA. Splits blocks by transition signature until stable. |
| `Trimmer` | Removes unreachable states (forward BFS) and useless states (backward BFS from accepting). Always preserves the initial state. |
| `AlphabetFragmenter` | Alphabet fragmentation: `IntRange`-labeled DFA -> `FragmentedAutomaton`. Collects `(source, target)` pairs per transition, partitions via `IntRange.partition`, assigns fragment ID = index. |

### `range` : Integer range classifiers

| Type | Description |
|---|---|
| `IntRange` | Record `(int lo, int hi)`, inclusive. Static algorithms: `normalize` (merge overlapping), `negate` (complement), `partition` (sweep-line splitting of labeled ranges). |
| `Classifier` | `@FunctionalInterface`: `int classify(int c)`, returns `-1` for unknown inputs. |
| `LinearClassifier` | O(n) linear scan over a sorted `IntRange` list. |
| `BinarySearchClassifier` | O(log n) binary search over a sorted `IntRange` list. |
| `TableClassifier` | O(1) flat-array lookup. Suitable for bounded alphabets (e.g. ASCII). |
| `FragmentedAutomaton<S>` | Record `(Automaton<S, Integer> dfa, Classifier classifier)`. Output of `AlphabetFragmenter`. |

## Pipelines

**IntRange-label pipeline** (used by `kuneiphorm-regex`):

```
Expression<IntRange> -> NFA (ExpressionConverter)
  -> DFA (RangeDeterminizer)
    -> trimmed DFA (Trimmer)
      -> minimized DFA (Minimizer)
        -> FragmentedAutomaton (AlphabetFragmenter)
```

**Discrete-label pipeline** (for non-range labels):

```
Expression<L> -> NFA (ExpressionConverter)
  -> DFA (Determinizer.determinize)
    -> minimized DFA (Minimizer)
```

## Key design decisions

- **`Expression<L>` is generic over the unit label type.** The label is opaque to daedalus; its meaning is defined by the consuming module.
- **`Pair<A, B>` replaces `Map.Entry`.** Used for transitions, labeled ranges, and partition results. Cleaner than `AbstractMap.SimpleEntry`.
- **`IntRange.partition` is the sweep-line algorithm.** Mirrors the `LabeledRange.normalize` algorithm from the reference codebase. Operates on `List<Pair<IntRange, Set<L>>>`, producing non-overlapping sub-ranges with label-set unions.
- **`AlphabetFragmenter` uses "fragmentation" lexicon.** Fragment ID = index in the partitioned list. No deduplication in the classifier -- deduplication is a runtime optimization for flat transition tables.
- **No code generation.** Daedalus only produces data structures. Code generation is handled by separate modules.
- **No visitor pattern.** Java 21 sealed interfaces + pattern matching switch enforce exhaustive dispatch.

## Requirements

- Java 21+
- No external dependencies
