package org.apache.metron.profiler.client.stellar;

import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.StandAloneProfiler;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.Util.getArg;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

/**
 * Stellar functions that allow interaction with the core Profiler components
 * through the Stellar REPL.
 */
public class ProfilerFunctions {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Stellar(
          namespace="PROFILER",
          name="INIT",
          description="Creates a local profile runner that can execute profiles.",
          params={
                  "config", "The profiler configuration as a string."
          },
          returns="A local profile runner."
  )
  public static class ProfilerInit implements StellarFunction {

    @Override
    public void initialize(Context context) {
    }

    @Override
    public boolean isInitialized() {
      return true;
    }

    @Override
    public Object apply(List<Object> args, Context context) {
      @SuppressWarnings("unchecked")
      Map<String, Object> global = (Map<String, Object>) context.getCapability(GLOBAL_CONFIG).orElseGet(null);

      // how long is the profile period?
      long duration = PROFILER_PERIOD.get(global, Long.class);
      String configuredUnits = PROFILER_PERIOD_UNITS.get(global, String.class);
      long periodDurationMillis = TimeUnit.valueOf(configuredUnits).toMillis(duration);

      // user must provide the configuration for the profiler
      String arg0 = getArg(0, String.class, args);
      ProfilerConfig profilerConfig;
      try {
        profilerConfig = JSONUtils.INSTANCE.load(arg0, ProfilerConfig.class);

      } catch(IOException e) {
        throw new IllegalArgumentException("Invalid profiler configuration", e);
      }

      return new StandAloneProfiler(profilerConfig, periodDurationMillis, context);
    }
  }

  @Stellar(
          namespace="PROFILER",
          name="APPLY",
          description="Apply a message to a local profile runner.",
          params={
                  "message", "The message to apply.",
                  "profiler", "A local profile runner returned by PROFILER_INIT."
          },
          returns="The local profile runner."
  )
  public static class ProfilerApply implements StellarFunction {

    private JSONParser parser;

    @Override
    public void initialize(Context context) {
      parser = new JSONParser();
    }

    @Override
    public boolean isInitialized() {
      return parser != null;
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // user must provide the json telemetry message
      String arg0 = Util.getArg(0, String.class, args);
      if(arg0 == null) {
        throw new IllegalArgumentException(format("expected string, found null"));
      }

      // parse the message
      JSONObject message;
      try {
        message = (JSONObject) parser.parse(arg0);

      } catch(org.json.simple.parser.ParseException e) {
        throw new IllegalArgumentException("invalid message", e);
      }

      // user must provide the stand alone profiler
      StandAloneProfiler profiler = Util.getArg(1, StandAloneProfiler.class, args);
      try {
        profiler.apply(message);

      } catch(ExecutionException e) {
        throw new IllegalArgumentException(e);
      }

      return profiler;
    }
  }

  @Stellar(
          namespace="PROFILER",
          name="FLUSH",
          description="Flush a local profile runner.",
          params={
                  "profiler", "A local profile runner returned by PROFILER_INIT."
          },
          returns="A list of the profile values."
  )
  public static class ProfilerFlush implements StellarFunction {

    @Override
    public void initialize(Context context) {
    }

    @Override
    public boolean isInitialized() {
      return true;
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // user must provide the stand-alone profiler
      StandAloneProfiler profiler = Util.getArg(0, StandAloneProfiler.class, args);
      if(profiler == null) {
        throw new IllegalArgumentException(format("expected the profiler returned by PROFILER_INIT, found null"));
      }

      // return the 'value' from each profile measurement
      List<ProfileMeasurement> measurements = profiler.flush();
      return measurements.stream().map(m -> m.getProfileValue()).collect(Collectors.toList());
    }
  }
}
