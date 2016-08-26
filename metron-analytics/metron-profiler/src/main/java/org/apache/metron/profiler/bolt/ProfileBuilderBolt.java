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

import backtype.storm.Config;
import backtype.storm.Constants;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
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

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;

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
   * The number of seconds between when the Profile is flushed.
   */
  private int flushFrequency;

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

  /**
   * Stellar context
   */
  private Context stellarContext;

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
    Config conf = new Config();
    conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, getFlushFrequency());
    return conf;
  }

  protected void initializeStellar() {
    stellarContext = new Context.Builder()
                         .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                         .build();
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
    declarer.declare(new Fields("measurement"));
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

      // if this is the first tuple in a window period, initialization is needed
      if (!isInitialized()) {
        init(input);
      }

      // update the profile with data from a new message
      update(input);

    } else {

      // flush the profile - can only flush if it has been initialized
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
    measurement = new ProfileMeasurement();
    measurement.setStart(getTimestamp());
    measurement.setEntity(input.getStringByField("entity"));
    measurement.setProfileName(profileConfig.getProfile());

    // execute the 'init' expression
    try {
      JSONObject message = (JSONObject) input.getValueByField("message");
      Map<String, String> expressions = profileConfig.getInit();
      expressions.forEach((var, expr) -> executor.assign(var, expr, message, stellarContext));

    } catch(ParseException e) {
      String msg = format("Bad 'init' expression: %s, profile=%s, entity=%s, start=%d",
              e.getMessage(), measurement.getProfileName(), measurement.getEntity(), measurement.getStart());
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
      expressions.forEach((var, expr) -> executor.assign(var, expr, message, stellarContext));

    } catch(ParseException e) {
      String msg = format("Bad 'update' expression: %s, profile=%s, entity=%s, start=%d",
              e.getMessage(), measurement.getProfileName(), measurement.getEntity(), measurement.getStart());
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
    LOG.info(String.format("Flushing profile: profile=%s, entity=%s, start=%d",
            measurement.getProfileName(), measurement.getEntity(), measurement.getStart()));

    // execute the 'result' expression
    Object result;
    try {
      String resultExpr = profileConfig.getResult();
      result = executor.execute(resultExpr, new JSONObject(), Object.class, stellarContext);
    
    } catch(ParseException e) {
      throw new ParseException("Bad 'result' expression", e);
    }

    // emit the completed profile measurement
    measurement.setEnd(getTimestamp());
    measurement.setValue(result);
    emit(measurement, tickTuple);

    // clear the execution state to prepare for the next window
    executor.clearState();

    // reset measurement - used as a flag to indicate if initialized
    measurement = null;
  }

  /**
   * Emits a message containing a ProfileMeasurement.
   * @param measurement The completed ProfileMeasurement.
   */
  private void emit(ProfileMeasurement measurement, Tuple anchor) {
    collector.emit(anchor, new Values(measurement));
    collector.ack(anchor);
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

  public int getFlushFrequency() {
    return flushFrequency;
  }

  public void setFlushFrequency(int flushFrequency) {
    this.flushFrequency = flushFrequency;
  }
}
