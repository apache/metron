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

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.apache.metron.common.bolt.ConfiguredProfilerBolt;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
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

/**
 * The bolt responsible for filtering incoming messages and directing
 * each to the one or more bolts responsible for building a Profile.  Each
 * message may be needed by 0, 1 or even many Profiles.
 */
public class ProfileSplitterBolt extends ConfiguredProfilerBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private OutputCollector collector;

  /**
   * JSON parser.
   */
  private transient JSONParser parser;

  /**
   * Executes Stellar code.
   */
  private StellarStatefulExecutor executor;

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
    this.executor = new DefaultStellarStatefulExecutor();
    initializeStellar();
  }

  protected void initializeStellar() {
    Context context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
            .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
            .build();
    StellarFunctions.initialize(context);
    executor.setContext(context);
  }

  @Override
  public void execute(Tuple input) {
    try {
      doExecute(input);

    } catch (IllegalArgumentException | ParseException | UnsupportedEncodingException e) {
      LOG.error("Unexpected failure: message='{}', tuple='{}'", e.getMessage(), input, e);
      collector.reportError(e);

    } finally {
      collector.ack(input);
    }
  }

  private void doExecute(Tuple input) throws ParseException, UnsupportedEncodingException {

    // retrieve the input message
    byte[] data = input.getBinary(0);
    JSONObject message = (JSONObject) parser.parse(new String(data, "UTF8"));

    // ensure there is a valid profiler configuration
    ProfilerConfig config = getProfilerConfig();
    if(config != null) {

      // apply the message to each of the profile definitions
      for (ProfileConfig profile: config.getProfiles()) {
        applyProfile(profile, input, message);
      }

    } else {
      LOG.warn("No Profiler configuration found.  Nothing to do.");
    }
  }

  /**
   * Applies a message to a Profile definition.
   * @param profile The profile definition.
   * @param input The input tuple that delivered the message.
   * @param message The message that may be needed by the profile.
   */
  private void applyProfile(ProfileConfig profile, Tuple input, JSONObject message) throws ParseException, UnsupportedEncodingException {
    @SuppressWarnings("unchecked")
    Map<String, Object> state = (Map<String, Object>)message;

    // is this message needed by this profile?
    if (executor.execute(profile.getOnlyif(), state, Boolean.class)) {

      // what is the name of the entity in this message?
      String entity = executor.execute(profile.getForeach(), state, String.class);

      // emit a message for the bolt responsible for building this profile
      collector.emit(input, new Values(entity, profile, message));
    }
  }

  /**
   * Each emitted tuple contains the following fields.
   * <p>
   * <ol>
   * <li> entity - The name of the entity.  The actual result of executing the Stellar expression.
   * <li> profile - The profile definition that the message needs applied to.
   * <li> message - The message containing JSON-formatted data that needs applied to a profile.
   * </ol>
   * <p>
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("entity", "profile", "message"));
  }

  public StellarStatefulExecutor getExecutor() {
    return executor;
  }

  public void setExecutor(StellarStatefulExecutor executor) {
    this.executor = executor;
  }
}
