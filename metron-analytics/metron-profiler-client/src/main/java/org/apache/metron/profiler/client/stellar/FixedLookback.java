package org.apache.metron.profiler.client.stellar;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.profiler.ProfilePeriod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Stellar(
      namespace="PROFILE",
      name="FIXED",
      description="The timestamps associated with a fixed lookback starting from now.",
      params={
        "durationAgo - How long ago should values be retrieved from?",
        "units - The units of 'durationAgo'.",
        "config_overrides - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                "of the same name. Default is the empty Map, meaning no overrides."
      },
      returns="The selected profile measurement timestamps."
)
public class FixedLookback implements StellarFunction {

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    Optional<Map> configOverridesMap = Optional.empty();
    long durationAgo = Util.getArg(0, Long.class, args);
    String unitsName = Util.getArg(1, String.class, args);
    TimeUnit units = TimeUnit.valueOf(unitsName);
    if(args.size() > 2) {
      Map rawMap = Util.getArg(2, Map.class, args);
      configOverridesMap = rawMap == null || rawMap.isEmpty() ? Optional.empty() : Optional.of(rawMap);
    }
    Map<String, Object> effectiveConfigs = Util.getEffectiveConfig(context, configOverridesMap.orElse(null));
    Long tickDuration = ProfilerConfig.PROFILER_PERIOD.get(effectiveConfigs, Long.class);
    TimeUnit tickUnit = TimeUnit.valueOf(ProfilerConfig.PROFILER_PERIOD_UNITS.get(effectiveConfigs, String.class));
    long end = System.currentTimeMillis();
    long start = end - units.toMillis(durationAgo);
    return ProfilePeriod.visitTimestamps(start, end, tickDuration, tickUnit, Optional.empty(), ts -> ts);
  }

  @Override
  public void initialize(Context context) {

  }

  @Override
  public boolean isInitialized() {
    return true;
  }
}
