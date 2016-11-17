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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.tools.javac.jvm.Profile;
import org.apache.commons.beanutils.BeanMap;
import org.apache.metron.common.bolt.ConfiguredProfilerBolt;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.apache.storm.Config;
import org.apache.storm.Constants;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * A bolt that is responsible for building a Profile.
 *
 * This bolt maintains the state required to build a Profile.  When the window
 * period expires, the data is summarized as a ProfileMeasurement, all state is
 * flushed, and the ProfileMeasurement is emitted.
 *
 */
public class ProfileBuilderBolt extends ConfiguredProfilerBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(ProfileBuilderBolt.class);

  private OutputCollector collector;

  /**
   * The duration of each profile period in milliseconds.
   */
  private long periodDurationMillis;

  /**
   * If a message has not been applied to a Profile in this number of periods,
   * the Profile will be forgotten and its resources will be cleaned up.
   *
   * If the profile TTL is `10` and the period duration is `15 minutes`, then
   * any Profile that has not had a message applied in the previous 10 periods,
   * which is the same as `150 minutes`, will be eligible for deletion.
   *
   * If not otherwise set, defaults to 20.
   */
  private int profileTimeToLive = 20;

  /**
   * Maintains the state of a profile which is unique to a profile/entity pair.
   */
  private transient Cache<String, ProfileState> profileCache;

  /**
   * Parses JSON messages.
   */
  private transient JSONParser parser;

  /**
   * @param zookeeperUrl The Zookeeper URL that contains the configuration data.
   */
  public ProfileBuilderBolt(String zookeeperUrl) {
    super(zookeeperUrl);
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
    this.collector = collector;
    this.parser = new JSONParser();
    this.profileCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(periodDurationMillis * profileTimeToLive, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * The builder emits a single field, 'measurement', which contains a ProfileMeasurement. A
   * ProfileMeasurement is emitted when a time window expires and a flush occurs.
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // once the time window expires, a complete ProfileMeasurement is emitted
    declarer.declare(new Fields("measurement", "profile"));
  }

  @Override
  public void execute(Tuple input) {
    try {
      doExecute(input);

    } catch (Throwable e) {
      LOG.error(format("Unexpected failure: message='%s', tuple='%s'", e.getMessage(), input), e);
      collector.reportError(e);

    } finally {
      collector.ack(input);
    }
  }

  /**
   * Update the execution environment based on data contained in the
   * message.  If the tuple is a tick tuple, then flush the profile
   * and reset the execution environment.
   * @param input The tuple to execute.
   */
  private void doExecute(Tuple input) throws ExecutionException {

    if(isTickTuple(input)) {
      flush(input);

    } else {
      if (!isInitialized(input)) {
        init(input);
      }
      update(input);
    }
  }

  /**
   * Initialize the bolt.  Occurs when the first tuple is received at the start
   * of each window period.
   * @param input The input tuple
   */
  private void init(Tuple input) throws ExecutionException {

    ProfileState state = getProfileState(input);
    try {

      // the original telemetry message is provided as additional context for the 'update' expressions
      JSONObject message = getMessage(input);

      // execute the 'init' expression
      Map<String, String> expressions = state.getDefinition().getInit();
      expressions.forEach((var, expr) -> state.getExecutor().assign(var, expr, message));

    } catch(ParseException e) {

      // make it brilliantly clear that one of the 'init' expressions is bad
      ProfileMeasurement measurement = state.getMeasurement();
      String msg = format("Bad 'init' expression: %s, profile=%s, entity=%s",
              e.getMessage(), measurement.getProfileName(), measurement.getEntity());
      throw new ParseException(msg, e);
    }
  }

  /**
   * Update the Profile based on data contained in a new message.
   * @param input The tuple containing a new message.
   */
  private void update(Tuple input) throws ExecutionException {

    ProfileState state = getProfileState(input);
    try {

      // the original telemetry message is provided as additional context for the 'update' expressions
      JSONObject message = getMessage(input);

      // execute each of the 'update' expressions
      Map<String, String> expressions = state.getDefinition().getUpdate();
      expressions.forEach((var, expr) -> state.getExecutor().assign(var, expr, message));

    } catch(ParseException e) {

      // make it brilliantly clear that one of the 'update' expressions is bad
      ProfileMeasurement measurement = state.getMeasurement();
      String msg = format("Bad 'update' expression: %s, profile=%s, entity=%s",
              e.getMessage(), measurement.getProfileName(), measurement.getEntity());
      throw new ParseException(msg, e);
    }
  }

  /**
   * Flush the Profiles.
   *
   * Executed on a fixed time period when a tick tuple is received.  Completes
   * and emits the ProfileMeasurement.  Clears all state in preparation for
   * the next window period.
   */
  private void flush(Tuple tickTuple) {

    // flush each of the profiles maintain by this bolt
    profileCache.asMap().forEach((key, profileState) -> {

      ProfileMeasurement measurement = profileState.getMeasurement();
      StellarExecutor executor = profileState.getExecutor();
      ProfileConfig definition = profileState.getDefinition();
      LOG.info(String.format("Flushing profile: profile=%s, entity=%s", measurement.getProfileName(), measurement.getEntity()));

      // execute the 'result' and 'group by' expressions
      Object value = executeResult(definition.getResult(), executor, measurement);
      measurement.setValue(value);

      List<Object> groups = executeGroupBy(definition.getGroupBy(), executor, measurement);
      measurement.setGroups(groups);

      // emit the completed profile measurement
      emit(measurement, definition);

      // clear the execution state to prepare for the next window
      executor.clearState();
    });
  }

  /**
   * Create the state necessary to build a Profile.
   * @param tuple The tuple that needs applied to a profile.
   */
  private ProfileState createProfileState(Tuple tuple) {

    // extract the profile definition
    ProfileConfig profileDefinition = getProfileDefinition(tuple);

    // create the profile measurement which will be emitted at the end of the window period
    ProfileMeasurement measurement = new ProfileMeasurement(
            profileDefinition.getProfile(),
            getEntity(tuple),
            getTimestamp(),
            periodDurationMillis,
            TimeUnit.MILLISECONDS);

    // create the executor
    StellarExecutor executor = new DefaultStellarExecutor();
    Context context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
            .build();
    StellarFunctions.initialize(context);
    executor.setContext(context);

    // create the profile state which is maintained within a cache for a fixed period of time
    ProfileState state = new ProfileState();
    state.setExecutor(executor);
    state.setDefinition(profileDefinition);
    state.setMeasurement(measurement);

    return state;
  }

  /**
   * Executes the 'result' expression of a Profile.
   * @return The result of evaluating the 'result' expression.
   */
  private Object executeResult(String expression, StellarExecutor executor, ProfileMeasurement measurement) {
    Object result;
    try {
      result = executor.execute(expression, new JSONObject(), Object.class);

    } catch(ParseException e) {
      String msg = format("Bad 'result' expression: %s, profile=%s, entity=%s",
              e.getMessage(), measurement.getProfileName(), measurement.getEntity());
      throw new ParseException(msg, e);
    }
    return result;
  }


  /**
   * Executes each of the 'groupBy' expressions.  The result of each
   * expression are the groups used to sort the data as part of the
   * row key.
   * @param expressions The 'groupBy' expressions to execute.
   * @return The result of executing the 'groupBy' expressions.
   */
  private List<Object> executeGroupBy(List<String> expressions, StellarExecutor executor, ProfileMeasurement measurement) {
    List<Object> groups = new ArrayList<>();

    if(!isEmpty(expressions)) {
      try {
        // allows each 'groupBy' expression to refer to the fields of the ProfileMeasurement
        BeanMap measureAsMap = new BeanMap(measurement);

        for (String expr : expressions) {
          Object result = executor.execute(expr, measureAsMap, Object.class);
          groups.add(result);
        }

      } catch(Throwable e) {
        String msg = format("Bad 'groupBy' expression: %s, profile=%s, entity=%s",
                e.getMessage(), measurement.getProfileName(), measurement.getEntity());
        throw new ParseException(msg, e);
      }
    }

    return groups;
  }

  /**
   * Emits a message containing a ProfileMeasurement and the Profile configuration.
   * @param measurement The completed ProfileMeasurement.
   * @param definition The profile definition.
   */
  private void emit(ProfileMeasurement measurement, ProfileConfig definition) {
    collector.emit(new Values(measurement, definition));
  }

  /**
   * Builds the key that is used to lookup the ProfileState within the cache.
   * @param tuple A tuple.
   */
  private String cacheKey(Tuple tuple) {
    return String.format("%s:%s", getProfileDefinition(tuple).getProfile(), getEntity(tuple));
  }

  /**
   * Retrieves the state associated with a Profile.  If none exists, the state will
   * be initialized.
   * @param tuple The tuple.
   */
  protected ProfileState getProfileState(Tuple tuple) throws ExecutionException {
    return profileCache.get(cacheKey(tuple), () -> createProfileState(tuple));
  }

  /**
   * Extracts the profile definition from a tuple.
   * @param tuple The tuple sent by the splitter bolt.
   */
  private ProfileConfig getProfileDefinition(Tuple tuple) {
    ProfileConfig definition = (ProfileConfig) tuple.getValueByField("profile");
    if(definition == null) {
      throw new IllegalStateException("invalid tuple received: missing profile definition");
    }

    return definition;
  }

  /**
   * Extracts the name of the entity from a tuple.
   * @param tuple The tuple sent by the splitter bolt.
   */
  private String getEntity(Tuple tuple) {
    String entity = tuple.getStringByField("entity");
    if(entity == null) {
      throw new IllegalStateException("invalid tuple received: missing entity name");
    }

    return entity;
  }

  /**
   * Extracts the original telemetry message from a tuple.
   * @param input The tuple sent by the splitter bolt.
   */
  private JSONObject getMessage(Tuple input) {
    JSONObject message = (JSONObject) input.getValueByField("message");
    if(message == null) {
      throw new IllegalStateException("invalid tuple received: missing message");
    }

    return message;
  }

  /**
   * Returns a value that can be used as the current timestamp.  Allows subclasses
   * to override, if necessary.
   */
  private long getTimestamp() {
    return System.currentTimeMillis();
  }

  /**
   * Has the Stellar execution environment already been initialized
   * @return True, it it has been initialized.
   */
  private boolean isInitialized(Tuple tuple) {
    return profileCache.getIfPresent(cacheKey(tuple)) != null;
  }

  /**
   * Is this a tick tuple?
   * @param tuple The tuple
   */
  protected static boolean isTickTuple(Tuple tuple) {
    return Constants.SYSTEM_COMPONENT_ID.equals(tuple.getSourceComponent()) &&
            Constants.SYSTEM_TICK_STREAM_ID.equals(tuple.getSourceStreamId());
  }

  public ProfileBuilderBolt withPeriodDurationMillis(long periodDurationMillis) {
    this.periodDurationMillis = periodDurationMillis;
    return this;
  }

  public ProfileBuilderBolt withPeriodDuration(int duration, TimeUnit units) {
    return withPeriodDurationMillis(units.toMillis(duration));
  }

  public ProfileBuilderBolt withProfileTimeToLive(int ttl) {
    if(ttl < 1) {
      throw new IllegalStateException("invalid profile time to live; must be > 0");
    }
    this.profileTimeToLive = ttl;
    return this;
  }

}
