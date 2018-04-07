/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.zookeeper.ZKConfigurationsCache;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.common.configuration.ConfigurationType.GLOBAL;
import static org.apache.metron.common.configuration.ConfigurationType.INDEXING;
import static org.apache.metron.common.configuration.ConfigurationType.PARSER;
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;
import static org.apache.metron.common.configuration.ConfigurationsUtils.writeGlobalConfigToZookeeper;
import static org.apache.metron.common.configuration.ConfigurationsUtils.writeProfilerConfigToZookeeper;
import static org.apache.metron.common.configuration.ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper;
import static org.apache.metron.common.configuration.ConfigurationsUtils.writeSensorIndexingConfigToZookeeper;
import static org.apache.metron.common.configuration.ConfigurationsUtils.writeSensorParserConfigToZookeeper;

/**
 * Defines functions that enable modification of Metron configuration values.
 */
public class ConfigurationFunctions {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  /**
   * Retrieves the Zookeeper client from the execution context.
   *
   * @param context The execution context.
   * @return A Zookeeper client, if one exists.  Otherwise, an exception is thrown.
   */
  private static CuratorFramework getZookeeperClient(Context context) {

    Optional<Object> clientOpt = context.getCapability(Context.Capabilities.ZOOKEEPER_CLIENT, true);
    if(clientOpt.isPresent()) {
      return (CuratorFramework) clientOpt.get();

    } else {
      throw new IllegalStateException("Missing ZOOKEEPER_CLIENT; zookeeper connection required");
    }
  }

  /**
   * Get an argument from a list of arguments.
   *
   * @param index The index within the list of arguments.
   * @param clazz The type expected.
   * @param args All of the arguments.
   * @param <T> The type of the argument expected.
   */
  public static <T> T getArg(int index, Class<T> clazz, List<Object> args) {

    if(index >= args.size()) {
      throw new IllegalArgumentException(format("expected at least %d argument(s), found %d", index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }

  /**
   * Serializes a configuration object to the raw JSON.
   *
   * @param object The configuration object to serialize
   * @return
   */
  private static String toJSON(Object object) {

    if(object == null) {
      return null;
    }

    try {
      return JSONUtils.INSTANCE.toJSON(object, true);

    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Stellar(
          namespace = "CONFIG",
          name = "GET",
          description = "Retrieve a Metron configuration from zookeeper.",
          params = {
                  "type - One of ENRICHMENT, INDEXING, PARSER, GLOBAL, PROFILER",
                  "sensor - Sensor to retrieve (required for enrichment and parser, not used for profiler and global)",
                  "emptyIfNotPresent - If true, then return an empty, minimally viable config"
          },
          returns = "The String representation of the config in zookeeper")
  public static class ConfigGet implements StellarFunction {

    /**
     * Whether the function has been initialized.
     */
    private boolean initialized = false;

    /**
     * Caches configuration values stored in Zookeeper.
     */
    private ZKConfigurationsCache cache;

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String result;

      // the configuration type to write
      String arg0 = getArg(0, String.class, args);
      ConfigurationType type = ConfigurationType.valueOf(arg0);

      if(GLOBAL == type) {
        result = getGlobalConfig();

      } else if(PROFILER == type) {
        result = getProfilerConfig(args);

      } else if(ENRICHMENT == type) {
        result = getEnrichmentConfig(args);

      } else if(INDEXING == type) {
        result = getIndexingConfig(args);

      } else if (PARSER == type) {
        result = getParserConfig(args);

      } else {
        throw new IllegalArgumentException("Unexpected configuration type: " + type);
      }

      return result;
    }

    /**
     * Retrieves the Global configuration.
     *
     * @return The Global configuration.
     */
    private String getGlobalConfig() {
      String result;IndexingConfigurations config = cache.get(IndexingConfigurations.class);
      Map<String, Object> global = config.getGlobalConfig();
      result = toJSON(global);
      return result;
    }

    /**
     * Retrieves the Parser configuration.
     *
     * @param args The function arguments.
     * @return The Parser configuration.
     */
    private String getParserConfig(List<Object> args) {

      // retrieve the parser configurations
      ParserConfigurations config = cache.get(ParserConfigurations.class);
      if(config == null) {
        config = new ParserConfigurations();
      }

      // retrieve the enrichment config for the given sensor
      String sensor = getArg(1, String.class, args);
      SensorParserConfig sensorConfig = config.getSensorParserConfig(sensor);

      // provide empty/default config if one is not present?
      if(sensorConfig == null && emptyIfNotPresent(args)) {
        sensorConfig = new SensorParserConfig();
      }

     return toJSON(sensorConfig);
    }

    /**
     * Retrieve the Enrichment configuration.
     *
     * @param args The function arguments.
     * @return The Enrichment configuration as a JSON string.
     */
    private String getEnrichmentConfig(List<Object> args) {

      // retrieve the enrichment configurations
      EnrichmentConfigurations config = cache.get(EnrichmentConfigurations.class);
      if(config == null) {
        config = new EnrichmentConfigurations();
      }

      // retrieve the enrichment config for the given sensor
      String sensor = getArg(1, String.class, args);
      SensorEnrichmentConfig sensorConfig = config.getSensorEnrichmentConfig(sensor);

      // provide empty/default config if one is not present?
      if(sensorConfig == null && emptyIfNotPresent(args)) {
        sensorConfig = new SensorEnrichmentConfig();
      }

      return toJSON(sensorConfig);
    }

    /**
     * Retrieve the Indexing configuration.
     *
     * @param args The function arguments.
     * @return The Indexing configuration as a JSON string.
     */
    private String getIndexingConfig(List<Object> args) {

      // retrieve the indexing configurations
      IndexingConfigurations config = cache.get(IndexingConfigurations.class);
      if(config == null) {
        config = new IndexingConfigurations();
      }

      // retrieve the enrichment config for the given sensor
      String sensor = getArg(1, String.class, args);
      Map<String, Object> sensorConfig = config.getSensorIndexingConfig(sensor, emptyIfNotPresent(args));

      return toJSON(sensorConfig);
    }

    /**
     * Retrieve the Profiler configuration.
     *
     * @param args The function arguments.
     * @return The Profiler configuration as a JSON string.
     */
    private String getProfilerConfig(List<Object> args) {

      // retrieve the profiler configurations
      ProfilerConfigurations config = cache.get(ProfilerConfigurations.class);
      if(config == null) {
        config = new ProfilerConfigurations();
      }

      ProfilerConfig profilerConfig = config.getProfilerConfig();
      if(profilerConfig == null && emptyIfNotPresent(args)) {
        profilerConfig = new ProfilerConfig();
      }

      return toJSON(profilerConfig);
    }

    /**
     * Retrieves the 'emptyIfNotPresent' argument.
     *
     * <p>This determines whether a default configuration should be returned, if no
     * configuration is not present.  This defaults to true.
     *
     * @param args The function arguments.
     * @return The 'emptyIfNotPresent' argument.
     */
    private boolean emptyIfNotPresent(List<Object> args) {

      boolean emptyIfNotPresent = true;
      if(args.size() >= 3) {
        emptyIfNotPresent = getArg(2, Boolean.class, args);
      }
      return emptyIfNotPresent;
    }

    @Override
    public void initialize(Context context) {
      CuratorFramework zkClient = getZookeeperClient(context);
      cache = new ZKConfigurationsCache(zkClient);
      cache.start();
      initialized = true;
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }
  }

  @Stellar(
          namespace = "CONFIG",
          name = "PUT",
          description = "Updates a Metron config to Zookeeper.",
          params = {
                  "type - One of ENRICHMENT, INDEXING, PARSER, GLOBAL, PROFILER",
                  "config - The config (a string in JSON form) to update",
                  "sensor - Sensor to retrieve (required for enrichment and parser, not used for profiler and global)"
          },
          returns = "The String representation of the config in zookeeper")
  public static class ConfigPut implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // the configuration type to write
      String arg0 = getArg(0, String.class, args);
      ConfigurationType type = ConfigurationType.valueOf(arg0);

      // the configuration value to write
      String value = getArg(1, String.class, args);
      if(value != null) {

        CuratorFramework client = getZookeeperClient(context);
        try {

          if(GLOBAL == type) {
            writeGlobalConfigToZookeeper(value.getBytes(), client);

          } else if(PROFILER == type) {
            writeProfilerConfigToZookeeper(value.getBytes(), client);

          } else if(ENRICHMENT == type) {
            String sensor = getArg(2, String.class, args);
            writeSensorEnrichmentConfigToZookeeper(sensor, value.getBytes(), client);

          } else if(INDEXING == type) {
            String sensor = getArg(2, String.class, args);
            writeSensorIndexingConfigToZookeeper(sensor, value.getBytes(), client);

          } else if (PARSER == type) {
            String sensor = getArg(2, String.class, args);
            writeSensorParserConfigToZookeeper(sensor, value.getBytes(), client);
          }

        } catch(Exception e) {
          LOG.error("Unexpected exception: {}", e.getMessage(), e);
          throw new ParseException(e.getMessage());
        }
      }

      return null;
    }

    @Override
    public void initialize(Context context) {
      // nothing to do
    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }
}
