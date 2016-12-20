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

import com.google.common.collect.Iterables;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.metron.common.configuration.manager.ConfigurationManager;
import org.apache.metron.common.configuration.manager.ZkConfigurationManager;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.profiler.clock.Clock;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.metron.common.configuration.ConfigurationType.GLOBAL;
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;

/**
 * The bolt responsible for filtering incoming messages and directing
 * each to the one or more bolts responsible for building a Profile.  Each
 * message may be needed by 0, 1 or even many Profiles.
 */
public class ProfileSplitterBolt extends BaseRichBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(ProfileSplitterBolt.class);

  private OutputCollector collector;

  /**
   * Executes Stellar code.
   */
  private StellarExecutor executor;

  /**
   * The URL to connect to Zookeeper.
   */
  private String zookeeperUrl;

  /**
   * The name of a field containing the timestamp.
   */
  private String timestampField;

  /**
   * A clock is used to tell time.
   */
  private Clock clock;

  /**
   * A Zookeeper client.
   */
  private transient CuratorFramework zookeeperClient;

  /**
   * Managers the configuration required by this bolt.
   */
  private transient ConfigurationManager configurationManager;

  /**
   * JSON parser.
   */
  private transient JSONParser parser;

  /**
   * Each emitted tuple contains the following fields.
   * <p><ol>
   * <li> entity - The name of the entity.  The actual result of executing the Stellar expression.
   * <li> profile - The profile definition that the message needs applied to.
   * <li> message - The message containing JSON-formatted data that needs applied to a profile.
   * <li> timestamp - A timestamp for the message.
   * </ol></p>
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("entity", "profile", "message", "timestamp"));
  }

  @Override
  public void prepare(Map stormConf, TopologyContext topologyContext, OutputCollector collector) {
    try {
      this.collector = collector;
      this.parser = new JSONParser();

      // zookeeper client
      if(zookeeperClient == null) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.zookeeperClient = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
        this.zookeeperClient.start();
      }

      // access the global configuration in zookeeper
      if(configurationManager == null) {
        this.configurationManager = new ZkConfigurationManager(zookeeperClient)
                .with(GLOBAL.getZookeeperRoot())
                .with(PROFILER.getZookeeperRoot())
                .open();
      }

      // initialize stellar
      if(executor == null) {
        Context context = createContext(configurationManager);
        StellarFunctions.initialize(context);

        this.executor = new DefaultStellarExecutor();
        executor.setContext(context);
      }

    } catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void cleanup() {
    super.cleanup();

    CloseableUtils.closeQuietly(zookeeperClient);
    CloseableUtils.closeQuietly(configurationManager);
  }

  @Override
  public void execute(Tuple input) {
    try {
      doExecute(input);

    } catch (IllegalArgumentException | ParseException | IOException e) {
      LOG.error(format("Unexpected failure: %s, tuple='%s'", e.getMessage(), input), e);
      collector.reportError(e);

    } finally {
      collector.ack(input);
    }
  }

  private void doExecute(Tuple input) throws ParseException, IOException {

    // retrieve the profiler definition
    Optional<ProfilerConfig> config = configurationManager.get(PROFILER.getZookeeperRoot(), ProfilerConfig.class);
    if(config.isPresent()) {

      // update the execution environment
      Context context = createContext(configurationManager);
      executor.setContext(context);

      // retrieve the input message
      byte[] data = input.getBinary(0);
      JSONObject message = (JSONObject) parser.parse(new String(data, "UTF8"));

      // apply the message to each of the profile definitions
      for (ProfileConfig profile : config.get().getProfiles()) {
        applyToProfile(profile, message, input);
      }

    } else {
      LOG.warn("profiler configuration not found at '{}'", PROFILER.getZookeeperRoot());
    }
  }

  /**
   * Applies a message to a Profile definition.
   * @param profile The profile definition.
   * @param jsonMessage The message that may be needed by the profile.
   * @param tuple The input tuple that delivered the message.
   */
  private void applyToProfile(ProfileConfig profile, JSONObject jsonMessage, Tuple tuple) {
    @SuppressWarnings("unchecked")
    Map<String, Object> message = (Map<String, Object>) jsonMessage;

    try {
      // is this message needed by this profile?
      String onlyIf = profile.getOnlyif();
      if (isBlank(onlyIf) || executor.execute(onlyIf, message, Boolean.class)) {

        // what is the name of the entity in this message?
        String entity = executor.execute(profile.getForeach(), message, String.class);

        // send a message to the profile builder; a message CANNOT be sent without a timestamp
        Optional<Long> timestamp = getTimestampValue(jsonMessage);
        timestamp.ifPresent(time -> collector.emit(tuple, new Values(entity, profile, message, time)));

        LOG.debug("forwarding message to profile: profile='{}' message='{}' timestamp='{}'",
                profile.getProfile(),
                timestamp.orElseGet(() -> 0L),
                Iterables.toString(message.entrySet()));
      } else {

        LOG.debug("profile will ignore this message: profile='{}' message='{}'",
                profile.getProfile(),
                Iterables.toString(message.entrySet()));
      }

    } catch(Throwable e) {
      LOG.error(format("Failed to apply message to profile: '%s' profile='%s' message='%s'",
              e.getMessage(), profile.getProfile(), JSONObject.toJSONString(jsonMessage)), e);
      collector.reportError(e);
    }
  }

  /**
   * Provides the value with which the outgoing tuples are timestamped.
   *
   * The outgoing tuples are timestamped so that Storm's window and event-time processing
   * functionality can recognize the time of the message. The timestamp that is attached
   * to each message is what decides whether the Profiler is operating on processing time
   * or event time.
   *
   * @param message The telemetry message.
   */
  private Optional<Long> getTimestampValue(JSONObject message) {
    Long result = null;

    if(isBlank(timestampField)) {
      // use the clock's time. this allows the profiler to use 'processing time'
      result = clock.currentTimeMillis();

    } else if(message.containsKey(timestampField)) {
      // use the timestamp from the message. the profiler is using 'event time'
      result = ConversionUtils.convert( message.get(timestampField), Long.class);

    } else {
      // most likely scenario is that the message does not contain the specified timestamp field
      LOG.warn("missing timestamp field '{}': message will be ignored: message='{}'", timestampField, JSONObject.toJSONString(message));
    }

    return Optional.ofNullable(result);
  }

  /**
   * Builds the Stellar execution context.
   * @param configurationManager The configuration manager.
   */
  private Context createContext(ConfigurationManager configurationManager) throws IOException {

    // retrieve the global configuration
    Map global = configurationManager
            .get(GLOBAL.getZookeeperRoot(), Map.class)
            .orElse(Collections.emptyMap());

    return new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> zookeeperClient)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
            .build();
  }

  public StellarExecutor getExecutor() {
    return executor;
  }

  public ProfileSplitterBolt withExecutor(StellarExecutor executor) {
    this.executor = executor;
    return this;
  }

  public ProfileSplitterBolt withZookeeperUrl(String zookeeperUrl) {
    this.zookeeperUrl = zookeeperUrl;
    return this;
  }

  public ProfileSplitterBolt withZookeeperClient(CuratorFramework zookeeperClient) {
    this.zookeeperClient = zookeeperClient;
    return this;
  }

  public ProfileSplitterBolt withTimestampField(String timestampField) {
    this.timestampField = timestampField;
    return this;
  }

  public ProfileSplitterBolt withClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  public ProfileSplitterBolt withConfigurationManager(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
    return this;
  }
}
