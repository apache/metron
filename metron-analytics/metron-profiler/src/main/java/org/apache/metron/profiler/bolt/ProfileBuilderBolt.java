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

import org.apache.commons.beanutils.BeanMap;
import org.apache.storm.Config;
import org.apache.storm.Constants;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.metron.common.bolt.ConfiguredProfilerBolt;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * Each instance of this bolt is responsible for maintaining the state for a single
 * Profile-Entity pair.
 */
public class ProfileBuilderBolt extends ConfiguredProfilerBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(ProfileBuilderBolt.class);

  /**
   * Executes Stellar code and maintains state across multiple invocations.
   */
  private StellarExecutor executor;

  /**
   * The duration of each profile period in milliseconds.
   */
  private long periodDurationMillis;

  /**
   * A ProfileMeasurement is created and emitted each window period.  A Profile
   * itself is composed of many ProfileMeasurements.
   */
  private transient ProfileMeasurement measurement;

  /**
   * The definition of the Profile that the bolt is building.
   */
  private transient ProfileConfig profileConfig;

  /**
   * Parses JSON messages.
   */
  private transient JSONParser parser;

  private OutputCollector collector;

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

  protected void initializeStellar() {
    Context context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
            .build();
    StellarFunctions.initialize(context);
    executor.setContext(context);
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);
    this.collector = collector;
    this.parser = new JSONParser();
    initializeStellar();
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
  private void doExecute(Tuple input) {

    if(!isTickTuple(input)) {
      if (!isInitialized()) {
        init(input);
      }
      update(input);

    } else {
      if(isInitialized()) {
        flush(input);
      }
    }
  }

  /**
   * Initialize the bolt.  Occurs when the first tuple is received at the start
   * of each window period.
   * @param input The input tuple
   */
  private void init(Tuple input) {

    // save the profile definition - needed later during a flush
    profileConfig = (ProfileConfig) input.getValueByField("profile");

    // create the measurement which will be saved at the end of the window period
    measurement = new ProfileMeasurement(
            profileConfig.getProfile(),
            input.getStringByField("entity"),
            getTimestamp(),
            periodDurationMillis,
            TimeUnit.MILLISECONDS);

    // execute the 'init' expression
    try {
      JSONObject message = (JSONObject) input.getValueByField("message");
      Map<String, String> expressions = profileConfig.getInit();
      expressions.forEach((var, expr) -> executor.assign(var, expr, message));

    } catch(ParseException e) {
      String msg = format("Bad 'init' expression: %s, profile=%s, entity=%s",
              e.getMessage(), measurement.getProfileName(), measurement.getEntity());
      throw new ParseException(msg, e);
    }
  }

  /**
   * Update the Profile based on data contained in a new message.
   * @param input The tuple containing a new message.
   */
  private void update(Tuple input) {
    JSONObject message = (JSONObject) input.getValueByField("message");

    // execute each of the 'update' expressions
    try {
      Map<String, String> expressions = profileConfig.getUpdate();
      expressions.forEach((var, expr) -> executor.assign(var, expr, message));

    } catch(ParseException e) {
      String msg = format("Bad 'update' expression: %s, profile=%s, entity=%s",
              e.getMessage(), measurement.getProfileName(), measurement.getEntity());
      throw new ParseException(msg, e);
    }
  }

  /**
   * Flush the Profile.
   *
   * Executed on a fixed time period when a tick tuple is received.  Completes
   * and emits the ProfileMeasurement.  Clears all state in preparation for
   * the next window period.
   */
  private void flush(Tuple tickTuple) {
    LOG.info(String.format("Flushing profile: profile=%s, entity=%s",
            measurement.getProfileName(), measurement.getEntity()));

    // calculate the result
    Object result = executeResult(profileConfig.getResult());
    measurement.setValue(result);

    // calculate the groups
    List<Object> groups = executeGroupBy(profileConfig.getGroupBy());
    measurement.setGroups(groups);

    // emit the completed profile measurement
    emit(measurement, tickTuple);

    // Execute the update with the old state
    Map<String, String> tickUpdate = profileConfig.getTickUpdate();
    Map<String, Object> state = executor.getState();
    if(tickUpdate != null) {
      tickUpdate.forEach((var, expr) -> executor.assign(var, expr, Collections.singletonMap("result", result)));
    }
    // clear the execution state to prepare for the next window
    executor.clearState();
    //make sure that we bring along the update state
    if(tickUpdate != null) {
      tickUpdate.forEach((var, expr) -> executor.getState().put(var, state.get(var)));
    }

    // reset measurement - used as a flag to indicate if initialized
    measurement = null;
  }

  /**
   * Executes the 'result' expression of a Profile.
   * @return The result of evaluating the 'result' expression.
   */
  private Object executeResult(String expression) {
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
  private List<Object> executeGroupBy(List<String> expressions) {
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
   * @param anchor The original tuple to be used as the anchor.
   */
  private void emit(ProfileMeasurement measurement, Tuple anchor) {
    collector.emit(anchor, new Values(measurement, profileConfig));
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
  private boolean isInitialized() {
    return measurement != null;
  }

  /**
   * Is this a tick tuple?
   * @param tuple The tuple
   */
  protected static boolean isTickTuple(Tuple tuple) {
    return Constants.SYSTEM_COMPONENT_ID.equals(tuple.getSourceComponent()) &&
            Constants.SYSTEM_TICK_STREAM_ID.equals(tuple.getSourceStreamId());
  }

  public StellarExecutor getExecutor() {
    return executor;
  }

  public void setExecutor(StellarExecutor executor) {
    this.executor = executor;
  }

  public void setPeriodDurationMillis(long periodDurationMillis) {
    this.periodDurationMillis = periodDurationMillis;
  }

  public void withPeriodDuration(int duration, TimeUnit units) {
    setPeriodDurationMillis(units.toMillis(duration));
  }
}
