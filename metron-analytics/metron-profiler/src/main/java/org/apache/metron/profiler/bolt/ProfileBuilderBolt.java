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
import com.google.common.collect.Iterables;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.metron.common.configuration.manager.ConfigurationManager;
import org.apache.metron.common.configuration.manager.ZkConfigurationManager;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.profiler.ProfileBuilder;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.metron.common.configuration.ConfigurationType.GLOBAL;
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;

/**
 * A bolt that is responsible for building a Profile.
 *
 * This bolt maintains the state required to build a Profile.  When the window
 * period expires, the data is summarized as a ProfileMeasurement, all state is
 * flushed, and the ProfileMeasurement is emitted.
 *
 */
public class ProfileBuilderBolt extends BaseWindowedBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(ProfileBuilderBolt.class);

  private OutputCollector collector;

  /**
   * A profile will be forgotten and its resources cleaned-up, if a message has
   * not been applied to it in within a fixed time frame.  The value is expressed
   * in milliseconds.
   *
   * WARNING: The TTL must be greater than the period duration.
   */
  private long timeToLiveMillis;

  /**
   * The duration of each profile period in milliseconds.
   *
   * This value is needed by the ProfileBuilder to generate valid row keys.
   */
  private long periodDurationMillis;


  /**
   * The URL to connect to Zookeeper.
   */
  private String zookeeperUrl;

  /**
   * A Zookeeper client.
   */
  private transient CuratorFramework zookeeperClient;

  /**
   * Managers the configuration required by this bolt.
   */
  private transient ConfigurationManager configurationManager;

  /**
   * Parses JSON messages.
   */
  private transient JSONParser parser;

  /**
   * Maintains the state of a profile which is unique to a profile/entity pair.
   */
  private transient Cache<String, ProfileBuilder> profileCache;

  /**
   * Validate the configuration.
   * @param stormConf Storm's configuration values.
   */
  private void validate(Map stormConf) {

    final String key = "topology.message.timeout.secs";
    if(stormConf.containsKey(key)) {
      long topologyTimeoutMillis = TimeUnit.SECONDS.toMillis(ConversionUtils.convert(stormConf.get(key), Long.class));

      if (topologyTimeoutMillis < periodDurationMillis) {
        String msg = format("expected: topology timeout [%d] > period duration [%d]", topologyTimeoutMillis, periodDurationMillis);
        throw new IllegalStateException(msg);
      }
    }

    if(timeToLiveMillis < periodDurationMillis) {
      String msg = format("expected: profile time-to-live [%d] > period duration [%d]", timeToLiveMillis, periodDurationMillis);
      throw new IllegalStateException(msg);
    }
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);
    validate(stormConf);
    this.collector = collector;
    this.parser = new JSONParser();

    // a cache of objects that build each profile
    this.profileCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(timeToLiveMillis, TimeUnit.MILLISECONDS)
            .build();

    // zookeeper client
    if(zookeeperClient == null) {
      RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
      this.zookeeperClient = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
      this.zookeeperClient.start();
    }

    // manages the configuration in zookeeper
    if(configurationManager == null) {
      try {
        this.configurationManager = new ZkConfigurationManager(zookeeperClient)
                .with(PROFILER.getZookeeperRoot())
                .with(GLOBAL.getZookeeperRoot())
                .open();

      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Override
  public void cleanup() {
    super.cleanup();
    CloseableUtils.closeQuietly(zookeeperClient);
    CloseableUtils.closeQuietly(configurationManager);
  }

  /**
   * The builder emits a single field, 'measurement', which contains a ProfileMeasurement. A
   * ProfileMeasurement is emitted when a time window expires and a flush occurs.
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    super.declareOutputFields(declarer);

    // once the time window expires, a complete ProfileMeasurement is emitted
    declarer.declare(new Fields("measurement", "profile"));
  }

  @Override
  public void execute(TupleWindow window) {
    try {
      LOG.debug("Received window containing '{}' tuple(s)", window.get().size());

      // apply each tuple in the window to a profile
      final Context context = createContext(zookeeperClient, configurationManager);
      for(Tuple tuple : window.get()) {
        applyTuple(tuple, context);
      }

      // find the max timestamp in this window
      Long flushTimeMillis = window
              .get()
              .stream()
              .map(t -> (Long) t.getValueByField("timestamp"))
              .max(Long::compare)
              .orElse(0L);

      // flush each profile
      profileCache.asMap().forEach((key, builder) -> flushProfile(flushTimeMillis, builder, context));

      // cache maintenance
      profileCache.cleanUp();

    } catch (Throwable e) {
      LOG.error(format("Unexpected failure: %s", e.getMessage()), e);
      collector.reportError(e);
    }
  }

  /**
   * Applies a tuple to a profile.
   *
   * @param tuple The tuple containing a telemetry message that needs applied to a profile.
   * @param context the Stellar execution context.
   */
  private void applyTuple(Tuple tuple, Context context) {
    try {
      ProfileBuilder builder = getBuilder(tuple);
      JSONObject message = getField("message", tuple, JSONObject.class);

      LOG.debug("Applying message to profile; profile='{}:{}' message='{}'", builder.getProfileName(), builder.getEntity(), Iterables.toString(message.entrySet()));
      builder.apply(message, context);

    } catch(Throwable e) {
      LOG.error(format("Failed to apply message to profile: '%s' tuple='%s'", e.getMessage(), tuple), e);
      collector.reportError(e);
    }
  }

  /**
   * Flush a profile.
   *
   * @param flushTimeMillis The flush time in epoch milliseconds.
   * @param builder The builder responsible for the profile.
   * @param context the Stellar execution context.
   */
  private void flushProfile(Long flushTimeMillis, ProfileBuilder builder, Context context) {
    try {
      LOG.debug("Flushing profile; flushTimeMillis='{}' profile='{}:{}'", flushTimeMillis, builder.getProfileName(), builder.getEntity());

      ProfileMeasurement measurement = builder.flush(flushTimeMillis, context);
      collector.emit(new Values(measurement, builder.getDefinition()));

    } catch(Throwable e) {
      LOG.error(format("Failed to flush profile: '%s' profile='%s:%s'", e.getMessage(), builder.getProfileName(), builder.getEntity()), e);
      collector.reportError(e);
    }
  }

  /**
   * Builds the key that is used to lookup the ProfileBuilder within the cache.
   *
   * The key is composed of the profile name, entity name, and profile hash code.  The
   * hash code of the profile definition is used as part of the key to detect
   * changes to the profile definition that have been made by the user.
   *
   * If a user does change a profile definition, we want to start using a freshly minted
   * ProfileBuilder, rather than the original, whose state was built using the old
   * definition.  This gives us a clean cut-over to the new profile definition and
   * minimizes the chance of unexpected interactions between the old and
   * new profile definition.
   *
   * @param tuple A tuple.
   */
  private String cacheKey(Tuple tuple) {
    ProfileConfig definition = getField("profile", tuple, ProfileConfig.class);
    String entity = getField("entity", tuple, String.class);
    return format("%s:%s:%d", definition.getProfile(), entity, definition.hashCode());
  }

  /**
   * Retrieves the cached ProfileBuilder that is used to build and maintain the Profile.  If none exists,
   * one will be created and returned.
   * @param tuple The tuple.
   */
  protected ProfileBuilder getBuilder(Tuple tuple) throws ExecutionException, IOException {
    return profileCache.get(cacheKey(tuple), () -> createBuilder(tuple));
  }

  /**
   * Creates a new ProfileBuilder.
   * @param tuple The tuple which drives creation
   * @return
   */
  protected ProfileBuilder createBuilder(Tuple tuple) throws IOException {
    return new ProfileBuilder.Builder()
            .withDefinition(getField("profile", tuple, ProfileConfig.class))
            .withEntity(getField("entity", tuple, String.class))
            .withPeriodDurationMillis(periodDurationMillis)
            .withExecutor(new DefaultStellarExecutor())
            .build();
  }

  /**
   * Create the Stellar execution context.
   */
  private static Context createContext(CuratorFramework zkClient, ConfigurationManager configurationManager) throws IOException {

    // retrieve the global configuration
    Map global = configurationManager
            .get(GLOBAL.getZookeeperRoot(), Map.class)
            .orElse(Collections.emptyMap());

    Context context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> zkClient)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
            .build();
    StellarFunctions.initialize(context);

    return context;
  }

  /**
   * Retrieves an expected field from a Tuple.  If the field is missing an exception is thrown to
   * indicate a fatal error.
   * @param fieldName The name of the field.
   * @param tuple The tuple from which to retrieve the field.
   * @param clazz The type of the field value.
   * @param <T> The type of the field value.
   */
  private static <T> T getField(String fieldName, Tuple tuple, Class<T> clazz) {
    T value = ConversionUtils.convert(tuple.getValueByField(fieldName), clazz);
    if(value == null) {
      throw new IllegalStateException(format("invalid tuple received: missing field '%s'", fieldName));
    }

    return value;
  }

  public ProfileBuilderBolt withZookeeperUrl(String zookeeperUrl) {
    this.zookeeperUrl = zookeeperUrl;
    return this;
  }

  public ProfileBuilderBolt withZookeeperClient(CuratorFramework zookeeperClient) {
    this.zookeeperClient = zookeeperClient;
    return this;
  }

  public ProfileBuilderBolt withConfigurationManager(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
    return this;
  }

  public ProfileBuilderBolt withTimeToLive(int duration, TimeUnit units) {
    this.timeToLiveMillis = units.toMillis(duration);
    return this;
  }

  public ProfileBuilderBolt withPeriodDuration(int duration, TimeUnit units) {
    this.periodDurationMillis = units.toMillis(duration);
    return this;
  }

  protected long getPeriodDurationMillis() {
    return periodDurationMillis;
  }
}
