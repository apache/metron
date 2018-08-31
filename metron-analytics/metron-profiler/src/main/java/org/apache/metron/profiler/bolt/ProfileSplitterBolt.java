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

package org.apache.metron.profiler.bolt;

import org.apache.metron.common.bolt.ConfiguredProfilerBolt;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.profiler.DefaultMessageRouter;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.MessageRouter;
import org.apache.metron.profiler.clock.Clock;
import org.apache.metron.profiler.clock.ClockFactory;
import org.apache.metron.profiler.clock.DefaultClockFactory;
import org.apache.metron.stellar.dsl.Context;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Storm bolt responsible for filtering incoming messages and directing
 * each to the downstream bolts responsible for building a Profile.
 */
public class ProfileSplitterBolt extends ConfiguredProfilerBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The name of the tuple field containing the entity.
   *
   * This is the result of executing a profile's 'entity' Stellar expression within
   * the context of the telemetry message.
   */
  protected static final String ENTITY_TUPLE_FIELD = "entity";

  /**
   * The name of the tuple field containing the profile definition.
   */
  protected static final String PROFILE_TUPLE_FIELD = "profile";

  /**
   * The name of the tuple field containing the telemetry message.
   */
  protected static final String MESSAGE_TUPLE_FIELD = "message";

  /**
   * The name of the tuple field containing the timestamp of the telemetry message.
   *
   * <p>If a 'timestampField' has been configured, the timestamp was extracted
   * from a field within the telemetry message.  This enables event time processing.
   *
   * <p>If a 'timestampField' has not been configured, then the Profiler uses
   * processing time and the timestamp originated from the system clock.
   */
  protected static final String TIMESTAMP_TUPLE_FIELD = "timestamp";

  private OutputCollector collector;

  /**
   * JSON parser.
   */
  private transient JSONParser parser;

  /**
   * The router responsible for routing incoming messages.
   */
  private transient MessageRouter router;

  /**
   * @param zookeeperUrl The Zookeeper URL that contains the configuration for this bolt.
   */
  public ProfileSplitterBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);
    this.collector = collector;
    this.parser = new JSONParser();
    this.router = new DefaultMessageRouter(getStellarContext());
  }

  public Context getStellarContext() {
    Map<String, Object> global = getConfigurations().getGlobalConfig();
    return new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
            .with(Context.Capabilities.STELLAR_CONFIG, () -> global)
            .build();
  }

  /**
   * This bolt consumes telemetry messages and determines if the message is needed
   * by any of the profiles.  The message is then routed to one or more downstream
   * bolts that are responsible for building each profile
   *
   * <p>The outgoing tuples are timestamped so that Storm's window and event-time
   * processing functionality can recognize the time of each message.
   *
   * <p>The timestamp that is attached to each outgoing tuple is what decides if
   * the Profiler is operating on processing time or event time.
   *
   * @param input The tuple.
   */
  @Override
  public void execute(Tuple input) {
    try {
      doExecute(input);

    } catch (Throwable t) {
      LOG.error("Unexpected error", t);
      collector.reportError(t);

    } finally {
      collector.ack(input);
    }
  }

  private void doExecute(Tuple input) throws ParseException, UnsupportedEncodingException {

    // retrieve the input message
    byte[] data = input.getBinary(0);
    if(data == null) {
      LOG.debug("Received null message. Nothing to do.");
      return;
    }

    // ensure there is a valid profiler configuration
    ProfilerConfig config = getProfilerConfig();
    if(config == null || getProfilerConfig().getProfiles().size() == 0) {
      LOG.debug("No Profiler configuration found. Nothing to do.");
      return;
    }

    JSONObject message = (JSONObject) parser.parse(new String(data, "UTF8"));
    routeMessage(input, message, config);
  }

  /**
   * Route a message based on the Profiler configuration.
   * @param input The input tuple on which to anchor.
   * @param message The telemetry message.
   * @param config The Profiler configuration.
   */
  private void routeMessage(Tuple input, JSONObject message, ProfilerConfig config) {

    // emit a tuple for each 'route'
    List<MessageRoute> routes = router.route(message, config, getStellarContext());
    for (MessageRoute route : routes) {

      Values values = createValues(route);
      collector.emit(input, values);

      LOG.debug("Found route for message; profile={}, entity={}, timestamp={}",
              route.getProfileDefinition().getProfile(),
              route.getEntity(),
              route.getTimestamp());
    }

    LOG.debug("Found {} route(s) for message", routes.size());
  }

  /**
   * Each emitted tuple contains the following fields.
   * <p>
   * <ol>
   * <li>message - The message containing JSON-formatted data that needs applied to a profile.
   * <li>timestamp - The timestamp of the message.
   * <li>entity - The name of the entity.  The actual result of executing the Stellar expression.
   * <li>profile - The profile definition that the message needs applied to.
   * </ol>
   * <p>
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

    // the order here must match 'createValues'
    Fields fields = new Fields(MESSAGE_TUPLE_FIELD, TIMESTAMP_TUPLE_FIELD, ENTITY_TUPLE_FIELD, PROFILE_TUPLE_FIELD);
    declarer.declare(fields);
  }

  /**
   * Creates the {@link Values} attached to the outgoing tuple.
   *
   * @param route The route the message must take.
   * @return
   */
  private Values createValues(MessageRoute route) {

    // the order here must match `declareOutputFields`
    return new Values(route.getMessage(), route.getTimestamp(), route.getEntity(), route.getProfileDefinition());
  }

  protected MessageRouter getMessageRouter() {
    return router;
  }

  public void setRouter(MessageRouter router) {
    this.router = router;
  }
}
