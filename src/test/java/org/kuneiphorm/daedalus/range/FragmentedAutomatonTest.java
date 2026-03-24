package org.kuneiphorm.daedalus.range;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.daedalus.craft.AlphabetFragmenter;
import org.kuneiphorm.daedalus.craft.ExpressionConverter;
import org.kuneiphorm.daedalus.craft.Minimizer;
import org.kuneiphorm.daedalus.craft.RangeDeterminizer;

class FragmentedAutomatonTest {

  @Test
  void fragmentCount_delegatesToClassifier() {
    Expression<IntRange> expr =
        Expression.choice(
            Expression.unit(new IntRange(97, 97)), Expression.unit(new IntRange(98, 98)));
    FragmentedAutomaton<String> fa =
        AlphabetFragmenter.fragment(
            Minimizer.minimize(
                RangeDeterminizer.determinize(ExpressionConverter.build(expr, "TOKEN"), Map.of())));
    assertEquals(fa.classifier().fragmentCount(), fa.fragmentCount());
    assertTrue(fa.fragmentCount() > 0);
  }
}
