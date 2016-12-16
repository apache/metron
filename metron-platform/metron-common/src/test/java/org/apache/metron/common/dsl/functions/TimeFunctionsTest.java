package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.common.utils.StellarExecutor;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Stellar time functions.
 */
public class TimeFunctionsTest {

  private StellarExecutor executor;

  @Before
  public void setup() {
    FunctionResolver functionResolver = new SimpleFunctionResolver()
            .withClass(TimeFunctions.Millis.class);

    executor = new StellarExecutor()
            .withFunctionResolver(functionResolver);
  }

  private long toLong(String expression) {
    return executor.execute(expression, Long.class);
  }

  @Test
  public void testMillis() {
    {
      long expected = TimeUnit.MICROSECONDS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'MICROSECONDS')"));
      assertEquals(expected, toLong("MILLIS(15, 'microseconds')"));
    }
    {
      long expected = TimeUnit.SECONDS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'SECONDS')"));
      assertEquals(expected, toLong("MILLIS(15, 'seconds')"));
    }
    {
      long expected = TimeUnit.MINUTES.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'MINUTES')"));
      assertEquals(expected, toLong("MILLIS(15, 'minutes')"));
    }
    {
      long expected = TimeUnit.HOURS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'HOURS')"));
      assertEquals(expected, toLong("MILLIS(15, 'hours')"));
    }
    {
      long expected = TimeUnit.DAYS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'DAYS')"));
      assertEquals(expected, toLong("MILLIS(15, 'days')"));
    }
  }
}
