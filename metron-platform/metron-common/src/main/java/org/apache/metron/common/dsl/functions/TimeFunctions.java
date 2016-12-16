package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.utils.ConversionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.upperCase;

/**
 * Stellar functions that operate on time.
 */
public class TimeFunctions {

  @Stellar(name = "MILLIS",
          description = "Converts a time duration to milliseconds.",
          params = { "duration - The duration of time. ",
                  "units - The units of the time duration; seconds, minutes, hours" },
          returns = "The duration in milliseconds as a Long.")
  public static class Millis extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {

      long duration = getArg(0, args, Long.class);
      TimeUnit units = TimeUnit.valueOf(upperCase(getArg(1, args, String.class)));
      return units.toMillis(duration);
    }
  }

  /**
   * Get an argument from a list of arguments.
   * @param index The index within the list of arguments.
   * @param args All of the arguments.
   * @param clazz The type expected.
   * @param <T> The type of the argument expected.
   */
  private static <T> T getArg(int index, List<Object> args, Class<T> clazz) {
    if(index >= args.size()) {
      throw new IllegalArgumentException(format("expected at least %d argument(s), found %d", index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }
}
