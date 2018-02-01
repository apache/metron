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
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.profiler.DefaultMessageDistributor;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.utils.TupleUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * A bolt that is responsible for building a Profile.
 *
 * This bolt maintains the state required to build a Profile.  When the window
 * period expires, the data is summarized as a ProfileMeasurement, all state is
 * flushed, and the ProfileMeasurement is emitted.
 *
 */
public class ProfileBuilderBolt extends ConfiguredProfilerBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private OutputCollector collector;

  /**
   * The duration of each profile period in milliseconds.
   */
  private long periodDurationMillis;

  /**
   * If a message has not been applied to a Profile in this number of milliseconds,
   * the Profile will be forgotten and its resources will be cleaned up.
   *
   * WARNING: The TTL must be at least greater than the period duration.
   */
  private long profileTimeToLiveMillis;

  /**
   * Distributes messages to the profile builders.
   */
  private DefaultMessageDistributor messageDistributor;

  /**
   * Parses JSON messages.
   */
  private transient JSONParser parser;

  /**
   * The measurements produced by a profile can be written to multiple destinations.  Each
   * destination is handled by a separate `DestinationHandler`.
   */
  private List<DestinationHandler> destinationHandlers;

  /**
   * @param zookeeperUrl The Zookeeper URL that contains the configuration data.
   */
  public ProfileBuilderBolt(String zookeeperUrl) {
    super(zookeeperUrl);
    this.destinationHandlers = new ArrayList<>();
  }

  /**
   * Defines the frequency at which the bolt will receive tick tuples.  Tick tuples are
   * used to control how often a profile is flushed.
   */
  @Override
  public Map<String, Object> getComponentConfiguration() {
    // how frequently should the bolt receive tick tuples?
    Config conf = new Config();
    conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, TimeUnit.MILLISECONDS.toSeconds(periodDurationMillis));
    return conf;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);

    if(profileTimeToLiveMillis < periodDurationMillis) {
      throw new IllegalStateException(format(
              "invalid configuration: expect profile TTL (%d) to be greater than period duration (%d)",
              profileTimeToLiveMillis,
              periodDurationMillis));
    }
    this.collector = collector;
    this.parser = new JSONParser();
    this.messageDistributor = new DefaultMessageDistributor(periodDurationMillis, profileTimeToLiveMillis);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    if(destinationHandlers.size() == 0) {
      throw new IllegalStateException("At least one destination handler must be defined.");
    }

    // each destination will define its own stream
    destinationHandlers.forEach(dest -> dest.declareOutputFields(declarer));
  }

  private Context getStellarContext() {
    Map<String, Object> global = getConfigurations().getGlobalConfig();
    return new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
            .with(Context.Capabilities.STELLAR_CONFIG, () -> global)
            .build();
  }

  /**
   * Expect to receive either a tick tuple or a telemetry message that needs applied
   * to a profile.
   * @param input The tuple.
   */
  @Override
  public void execute(Tuple input) {
    try {
      if(TupleUtils.isTick(input)) {
        handleTick();

      } else {
        handleMessage(input);
      }

    } catch (Throwable e) {
      LOG.error(format("Unexpected failure: message='%s', tuple='%s'", e.getMessage(), input), e);
      collector.reportError(e);

    } finally {
      collector.ack(input);
    }
  }

  /**
   * Handles a telemetry message
   * @param input The tuple.
   */
  private void handleMessage(Tuple input) throws ExecutionException {
    JSONObject message = getField("message", input, JSONObject.class);
    ProfileConfig definition = getField("profile", input, ProfileConfig.class);
    String entity = getField("entity", input, String.class);
    MessageRoute route = new MessageRoute(definition, entity);

    messageDistributor.distribute(message, route, getStellarContext());
  }

  /**
   * Handles a tick tuple.
   */
  private void handleTick() {
    List<ProfileMeasurement> measurements = messageDistributor.flush();

    // forward the measurements to each destination handler
    for(ProfileMeasurement m : measurements ) {
      destinationHandlers.forEach(handler -> handler.emit(m, collector));
    }
  }

  /**
   * Retrieves an expected field from a Tuple.  If the field is missing an exception is thrown to
   * indicate a fatal error.
   * @param fieldName The name of the field.
   * @param tuple The tuple from which to retrieve the field.
   * @param clazz The type of the field value.
   * @param <T> The type of the field value.
   */
  private <T> T getField(String fieldName, Tuple tuple, Class<T> clazz) {
    T value = ConversionUtils.convert(tuple.getValueByField(fieldName), clazz);
    if(value == null) {
      throw new IllegalStateException(format("invalid tuple received: missing or invalid field '%s'", fieldName));
    }

    return value;
  }

  public ProfileBuilderBolt withPeriodDurationMillis(long periodDurationMillis) {
    this.periodDurationMillis = periodDurationMillis;
    return this;
  }

  public ProfileBuilderBolt withPeriodDuration(int duration, TimeUnit units) {
    return withPeriodDurationMillis(units.toMillis(duration));
  }

  public ProfileBuilderBolt withProfileTimeToLiveMillis(long timeToLiveMillis) {
    this.profileTimeToLiveMillis = timeToLiveMillis;
    return this;
  }

  public ProfileBuilderBolt withProfileTimeToLive(int duration, TimeUnit units) {
    return withProfileTimeToLiveMillis(units.toMillis(duration));
  }

  public ProfileBuilderBolt withDestinationHandler(DestinationHandler handler) {
    this.destinationHandlers.add(handler);
    return this;
  }

  public DefaultMessageDistributor getMessageDistributor() {
    return messageDistributor;
  }
}
