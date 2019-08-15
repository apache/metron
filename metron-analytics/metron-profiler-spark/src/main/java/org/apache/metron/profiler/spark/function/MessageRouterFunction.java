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
package org.apache.metron.profiler.spark.function;

import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.LazyLogger;
import org.apache.metron.common.utils.LazyLoggerFactory;
import org.apache.metron.profiler.DefaultMessageRouter;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.MessageRouter;
import org.apache.metron.profiler.clock.Clock;
import org.apache.metron.profiler.clock.ClockFactory;
import org.apache.metron.profiler.clock.EventTimeOnlyClockFactory;
import org.apache.metron.stellar.dsl.Context;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The function responsible for finding routes for a given message in Spark.
 */
public class MessageRouterFunction implements FlatMapFunction<String, MessageRoute> {

  protected static final LazyLogger LOG = LazyLoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The global configuration used for the execution of Stellar.
   */
  private Map<String, String> globals;

  /**
   * The profile definitions.
   */
  private ProfilerConfig profilerConfig;

  /**
   * A clock that can extract time from the messages received.
   */
  private Clock clock;

  /**
   * Only messages with a timestamp after this will be routed.
   */
  private Long begin;

  /**
   * Only messages with a timestamp before this will be routed.
   */
  private Long end;

  public MessageRouterFunction(ProfilerConfig profilerConfig, Map<String, String> globals) {
    this.profilerConfig = profilerConfig;
    this.globals = globals;
    this.begin = Long.MIN_VALUE;
    this.end = Long.MAX_VALUE;
    withClockFactory(new EventTimeOnlyClockFactory());
  }

  /**
   * Find all routes for a given telemetry message.
   *
   * <p>A message may need routed to multiple profiles should it be needed by more than one.  A
   * message may also not be routed should it not be needed by any profiles.
   *
   * @param jsonMessage The raw JSON message.
   * @return A list of message routes.
   */
  @Override
  public Iterator<MessageRoute> call(String jsonMessage) throws Exception {
    List<MessageRoute> routes = Collections.emptyList();
    JSONParser parser = new JSONParser();
    Context context = TaskUtils.getContext(globals);
    MessageRouter router = new DefaultMessageRouter(context);

    // parse the raw message
    Optional<JSONObject> message = toMessage(jsonMessage, parser);
    if(message.isPresent()) {

      // extract the timestamp from the message
      Optional<Long> timestampOpt = clock.currentTimeMillis(message.get());
      if (timestampOpt.isPresent()) {

        // timestamp must be in [begin, end]
        Long timestamp = timestampOpt.get();
        if(timestamp >= begin && timestamp <= end) {
          routes = router.route(message.get(), profilerConfig, context);
          LOG.trace("Found {} route(s) for a message", routes.size());

        } else {
          LOG.trace("Ignoring message; timestamp={} not in [{},{}]", () -> timestamp, () -> prettyPrint(begin), () -> prettyPrint(end));
        }

      } else {
        LOG.trace("No timestamp in message. Message will be ignored.");
      }

    } else {
      LOG.trace("Unable to parse message. Message will be ignored");
    }

    return routes.iterator();
  }

  /**
   * Set a time constraint.
   *
   * @param begin Only messages with a timestamp after this will be routed.
   * @return The message router function
   */
  public MessageRouterFunction withBegin(Long begin) {
    this.begin = begin;
    return this;
  }

  /**
   * Set a time constraint.
   *
   * @param end Only messages with a timestamp before this will be routed.
   * @return The message router function
   */
  public MessageRouterFunction withEnd(Long end) {
    this.end = end;
    return this;
  }

  /**
   * Defines the {@link ClockFactory} used to create the {@link Clock}.
   *
   * <p>Calling this method is only needed to override the default behavior.
   *
   * @param clockFactory The factory to use for creating the {@link Clock}.
   * @return The message router function.
   */
  public MessageRouterFunction withClockFactory(ClockFactory clockFactory) {
    this.clock = clockFactory.createClock(profilerConfig);
    return this;
  }

  /**
   * Parses the raw JSON of a message.
   *
   * @param json The raw JSON to parse.
   * @param parser The parser to use.
   * @return The parsed telemetry message.
   */
  private static Optional<JSONObject> toMessage(String json, JSONParser parser) {
    try {
      JSONObject message = (JSONObject) parser.parse(json);
      return Optional.of(message);

    } catch(Throwable e) {
      LOG.warn(String.format("Unable to parse message, message will be ignored; message='%s'", json), e);
      return Optional.empty();
    }
  }

  /**
   * Pretty prints a Long value for use when logging these values.
   *
   * <p>Long.MIN_VALUE and Long.MAX_VALUE will occur frequently and is difficult to grok in the logs.  Instead
   * Long.MIN_VALUE is rendered as "MIN". Long.MAX_VALUE is srendered as "MAX". All other values are rendered
   * directly.
   *
   * @param value The value to pretty print.
   * @return
   */
  private static String prettyPrint(Long value) {
    String result;
    if(value == Long.MIN_VALUE) {
      result = "MIN";
    } else if(value == Long.MAX_VALUE) {
      result = "MAX";
    } else {
      result = value.toString();
    }
    return result;
  }
}
