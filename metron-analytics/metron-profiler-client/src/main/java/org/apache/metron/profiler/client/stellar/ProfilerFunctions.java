/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.profiler.client.stellar;

import org.apache.commons.lang3.ClassUtils;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.StandAloneProfiler;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
      Map<String, Object> global = (Map<String, Object>) context.getCapability(GLOBAL_CONFIG, false)
              .orElse(Collections.emptyMap());

      // how long is the profile period?
      long duration = PROFILER_PERIOD.getOrDefault(global, PROFILER_PERIOD.getDefault(), Long.class);
      String configuredUnits = PROFILER_PERIOD_UNITS.getOrDefault(global, PROFILER_PERIOD_UNITS.getDefault(), String.class);
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
                  "message(s)", "The message to apply; a JSON string or list of JSON strings.",
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

      // the use can pass in one or more messages in a few different forms
      Object arg0 = Util.getArg(0, Object.class, args);
      List<JSONObject> messages = getMessages(arg0);

      // user must provide the stand alone profiler
      StandAloneProfiler profiler = Util.getArg(1, StandAloneProfiler.class, args);
      try {
        for (JSONObject message : messages) {
          profiler.apply(message);
        }

      } catch (ExecutionException e) {
        throw new IllegalArgumentException(format("Failed to apply message; error=%s", e.getMessage()), e);
      }

      return profiler;
    }

    /**
     * Gets a message or messages from the function arguments.
     *
     * @param arg The function argument containing the message(s).
     * @return A list of messages
     */
    private List<JSONObject> getMessages(Object arg) {
      List<JSONObject> messages;

      if (arg instanceof String) {
        messages = getMessagesFromString((String) arg);

      } else if (arg instanceof Iterable) {
        messages = getMessagesFromIterable((Iterable<String>) arg);

      } else if (arg instanceof JSONObject) {
        messages = Collections.singletonList((JSONObject) arg);

      } else {
        throw new IllegalArgumentException(format("invalid message: found '%s', expected String, List, or JSONObject",
                ClassUtils.getShortClassName(arg, "null")));
      }

      return messages;
    }

    /**
     * Gets a message or messages from a List
     *
     * @param strings The function argument that is a bunch of strings.
     * @return A list of messages.
     */
    private List<JSONObject> getMessagesFromIterable(Iterable<String> strings) {
      List<JSONObject> messages = new ArrayList<>();

      // the user pass in a list of strings
      for (String str : strings) {
        messages.addAll(getMessagesFromString(str));
      }

      return messages;
    }

    /**
     * Gets a message or messages from a String argument.
     *
     * @param arg0 The function argument is just a List.
     * @return A list of messages.
     */
    private List<JSONObject> getMessagesFromString(String arg0) {
      List<JSONObject> messages = new ArrayList<>();

      try {
        Object parsedArg0 = parser.parse(arg0);
        if (parsedArg0 instanceof JSONObject) {
          // if the string only contains one message
          messages.add((JSONObject) parsedArg0);

        } else if (parsedArg0 instanceof JSONArray) {
          // if the string contains multiple messages
          JSONArray jsonArray = (JSONArray) parsedArg0;
          for (Object item : jsonArray) {
            messages.addAll(getMessages(item));
          }

        } else {
          throw new IllegalArgumentException(format("invalid message: found '%s', expected JSONObject or JSONArray",
                  ClassUtils.getShortClassName(parsedArg0, "null")));
        }

      } catch (org.json.simple.parser.ParseException e) {
        throw new IllegalArgumentException(format("invalid message: '%s'", e.getMessage()), e);
      }

      return messages;
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

      // transform the profile measurements into maps to simplify manipulation in stellar
      List<Map<String, Object>> measurements = new ArrayList<>();
      for(ProfileMeasurement m : profiler.flush()) {

        // create a map for the profile period
        Map<String, Object> period = new HashMap<>();
        period.put("period", m.getPeriod().getPeriod());
        period.put("start", m.getPeriod().getStartTimeMillis());
        period.put("duration", m.getPeriod().getDurationMillis());
        period.put("end", m.getPeriod().getEndTimeMillis());

        // create a map for the measurement
        Map<String, Object> measurement = new HashMap<>();
        measurement.put("profile", m.getProfileName());
        measurement.put("entity", m.getEntity());
        measurement.put("value", m.getProfileValue());
        measurement.put("groups", m.getGroups());
        measurement.put("period", period);

        measurements.add(measurement);
      }

      return measurements;
    }
  }
}
