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
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.log4j.Logger;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.common.utils.JSONUtils;

import java.util.*;

public class ConfigurationFunctions {
  private static final Logger LOG = Logger.getLogger(ConfigurationFunctions.class);
  private static EnumMap<ConfigurationType, Object> configMap = new EnumMap<ConfigurationType, Object>(ConfigurationType.class) {{
    for(ConfigurationType ct : ConfigurationType.values()) {
      put(ct, Collections.synchronizedMap(new HashMap<String, String>()));
    }
    put(ConfigurationType.GLOBAL, "");
    put(ConfigurationType.PROFILER, "");
  }};
  private static synchronized void setupTreeCache(Context context) throws Exception {
    try {
      Optional<Object> treeCacheOpt = context.getCapability("treeCache");
      if (treeCacheOpt.isPresent()) {
        return;
      }
    }
    catch(IllegalStateException ex) {

    }
    Optional<Object> clientOpt = context.getCapability(Context.Capabilities.ZOOKEEPER_CLIENT);
    if(!clientOpt.isPresent()) {
      throw new IllegalStateException("I expected a zookeeper client to exist and it did not.  Please connect to zookeeper.");
    }
    CuratorFramework client = (CuratorFramework) clientOpt.get();
    TreeCache cache = new TreeCache(client, Constants.ZOOKEEPER_TOPOLOGY_ROOT);
    TreeCacheListener listener = new TreeCacheListener() {
      @Override
      public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        if (event.getType().equals(TreeCacheEvent.Type.NODE_ADDED) || event.getType().equals(TreeCacheEvent.Type.NODE_UPDATED)) {
          String path = event.getData().getPath();
          byte[] data = event.getData().getData();
          String sensor = Iterables.getLast(Splitter.on("/").split(path), null);
          if (path.startsWith(ConfigurationType.PARSER.getZookeeperRoot())) {
            Map<String, String> sensorMap = (Map<String, String>)configMap.get(ConfigurationType.PARSER);
            sensorMap.put(sensor, new String(data));
          } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
            configMap.put(ConfigurationType.GLOBAL, new String(data));
          } else if (ConfigurationType.PROFILER.getZookeeperRoot().equals(path)) {
            configMap.put(ConfigurationType.PROFILER, new String(data));
          } else if (path.startsWith(ConfigurationType.ENRICHMENT.getZookeeperRoot())) {
            Map<String, String> sensorMap = (Map<String, String>)configMap.get(ConfigurationType.ENRICHMENT);
            sensorMap.put(sensor, new String(data));
          } else if (path.startsWith(ConfigurationType.INDEXING.getZookeeperRoot())) {
            Map<String, String> sensorMap = (Map<String, String>)configMap.get(ConfigurationType.INDEXING);
            sensorMap.put(sensor, new String(data));
          }
        }
        else if(event.getType().equals(TreeCacheEvent.Type.NODE_REMOVED)) {
          String path = event.getData().getPath();
          String sensor = Iterables.getLast(Splitter.on("/").split(path), null);
          if (path.startsWith(ConfigurationType.PARSER.getZookeeperRoot())) {
            Map<String, String> sensorMap = (Map<String, String>)configMap.get(ConfigurationType.PARSER);
            sensorMap.remove(sensor);
          }
          else if (path.startsWith(ConfigurationType.ENRICHMENT.getZookeeperRoot())) {
            Map<String, String> sensorMap = (Map<String, String>)configMap.get(ConfigurationType.ENRICHMENT);
            sensorMap.remove(sensor);
          }
          else if (path.startsWith(ConfigurationType.INDEXING.getZookeeperRoot())) {
            Map<String, String> sensorMap = (Map<String, String>)configMap.get(ConfigurationType.INDEXING);
            sensorMap.remove(sensor);
          }
          else if (ConfigurationType.PROFILER.getZookeeperRoot().equals(path)) {
            configMap.put(ConfigurationType.PROFILER, null);
          }
          else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
            configMap.put(ConfigurationType.GLOBAL, null);
          }
        }
      }
    };
    cache.getListenable().addListener(listener);
    cache.start();
    for(ConfigurationType ct : ConfigurationType.values()) {
      switch(ct) {
        case GLOBAL:
        case PROFILER:
          {
            String data = "";
            try {
              byte[] bytes = ConfigurationsUtils.readFromZookeeper(ct.getZookeeperRoot(), client);
              data = new String(bytes);
            }
            catch(Exception ex) {

            }
            configMap.put(ct, data);
          }
          break;
        case INDEXING:
        case ENRICHMENT:
        case PARSER:
          {
            List<String> sensorTypes = client.getChildren().forPath(ct.getZookeeperRoot());
            Map<String, String> sensorMap = (Map<String, String>)configMap.get(ct);
            for(String sensorType : sensorTypes) {
              sensorMap.put(sensorType, new String(ConfigurationsUtils.readFromZookeeper(ct.getZookeeperRoot() + "/" + sensorType, client)));
            }
          }
          break;
      }
    }
    context.addCapability("treeCache", () -> cache);
  }

  @Stellar(
           namespace = "CONFIG"
          ,name = "GET"
          ,description = "Retrieve a Metron configuration from zookeeper."
          ,params = {"type - One of ENRICHMENT, INDEXING, PARSER, GLOBAL, PROFILER"
                    , "sensor - Sensor to retrieve (required for enrichment and parser, not used for profiler and global)"
                    , "emptyIfNotPresent - If true, then return an empty, minimally viable config"
                    }
          ,returns = "The String representation of the config in zookeeper"
          )
  public static class ConfigGet implements StellarFunction {
    boolean initialized = false;
    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      ConfigurationType type = ConfigurationType.valueOf((String)args.get(0));
      boolean emptyIfNotPresent = true;

      switch(type) {
        case GLOBAL:
        case PROFILER:
          return configMap.get(type);
        case PARSER: {
          String sensor = (String) args.get(1);
          if(args.size() > 2) {
            emptyIfNotPresent = ConversionUtils.convert(args.get(2), Boolean.class);
          }
          Map<String, String> sensorMap = (Map<String, String>) configMap.get(type);
          String ret = sensorMap.get(sensor);
          if (ret == null && emptyIfNotPresent ) {
            SensorParserConfig config = new SensorParserConfig();
            config.setSensorTopic(sensor);
            try {
              ret = JSONUtils.INSTANCE.toJSON(config, true);
            } catch (JsonProcessingException e) {
              LOG.error("Unable to serialize default object: " + e.getMessage(), e);
              throw new ParseException("Unable to serialize default object: " + e.getMessage(), e);
            }
          }
          return ret;
        }
        case INDEXING: {
          String sensor = (String) args.get(1);
          if(args.size() > 2) {
            emptyIfNotPresent = ConversionUtils.convert(args.get(2), Boolean.class);
          }
          Map<String, String> sensorMap = (Map<String, String>) configMap.get(type);
          String ret = sensorMap.get(sensor);
          if (ret == null && emptyIfNotPresent ) {
            Map<String, Object> config = new HashMap<>();
            try {
              ret = JSONUtils.INSTANCE.toJSON(config, true);
              IndexingConfigurations.setIndex(config, sensor);
            } catch (JsonProcessingException e) {
              LOG.error("Unable to serialize default object: " + e.getMessage(), e);
              throw new ParseException("Unable to serialize default object: " + e.getMessage(), e);
            }
          }
          return ret;
        }
        case ENRICHMENT: {
          String sensor = (String) args.get(1);
          if(args.size() > 2) {
            emptyIfNotPresent = ConversionUtils.convert(args.get(2), Boolean.class);
          }
          Map<String, String> sensorMap = (Map<String, String>) configMap.get(type);
          String ret = sensorMap.get(sensor);
          if (ret == null && emptyIfNotPresent ) {
            SensorEnrichmentConfig config = new SensorEnrichmentConfig();
            try {
              ret = JSONUtils.INSTANCE.toJSON(config, true);
            } catch (JsonProcessingException e) {
              LOG.error("Unable to serialize default object: " + e.getMessage(), e);
              throw new ParseException("Unable to serialize default object: " + e.getMessage(), e);
            }
          }
          return ret;
        }
        default:
          throw new UnsupportedOperationException("Unable to support type " + type);
      }
    }

    @Override
    public void initialize(Context context) {
      try {
        setupTreeCache(context);
      } catch (Exception e) {
        LOG.error("Unable to initialize: " + e.getMessage(), e);
      }
      finally {
        initialized = true;
      }
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }
  }
  @Stellar(
           namespace = "CONFIG"
          ,name = "PUT"
          ,description = "Updates a Metron config to Zookeeper."
          ,params = {"type - One of ENRICHMENT, INDEXING, PARSER, GLOBAL, PROFILER"
                    ,"config - The config (a string in JSON form) to update"
                    , "sensor - Sensor to retrieve (required for enrichment and parser, not used for profiler and global)"
                    }
          ,returns = "The String representation of the config in zookeeper"
          )
  public static class ConfigPut implements StellarFunction {
    private CuratorFramework client;
    private boolean initialized = false;

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      ConfigurationType type = ConfigurationType.valueOf((String)args.get(0));
      String config = (String)args.get(1);
      if(config == null) {
        return null;
      }
      try {
        switch (type) {
          case GLOBAL:
            ConfigurationsUtils.writeGlobalConfigToZookeeper(config.getBytes(), client);
            break;
          case PROFILER:
            ConfigurationsUtils.writeProfilerConfigToZookeeper(config.getBytes(), client);
            break;
          case ENRICHMENT:
          {
            String sensor = (String) args.get(2);
            if(sensor == null) {
              return null;
            }
            ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensor, config.getBytes(), client);
          }
          break;
          case INDEXING:
          {
            String sensor = (String) args.get(2);
            if(sensor == null) {
              return null;
            }
            ConfigurationsUtils.writeSensorIndexingConfigToZookeeper(sensor, config.getBytes(), client);
          }
          break;
          case PARSER:
            {
            String sensor = (String) args.get(2);
              if(sensor == null) {
              return null;
            }
            ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensor, config.getBytes(), client);
          }
          break;
        }
      }
      catch(Exception ex) {
        LOG.error("Unable to put config: " + ex.getMessage(), ex);
        throw new ParseException("Unable to put config: " + ex.getMessage(), ex);
      }
      return null;
    }

    @Override
    public void initialize(Context context) {
      Optional<Object> clientOpt = context.getCapability(Context.Capabilities.ZOOKEEPER_CLIENT);
      if(!clientOpt.isPresent()) {
        throw new IllegalStateException("I expected a zookeeper client to exist and it did not.  Please connect to zookeeper.");
      }
      client = (CuratorFramework) clientOpt.get();
      try {
        setupTreeCache(context);
      } catch (Exception e) {
        LOG.error("Unable to initialize: " + e.getMessage(), e);
      }
      finally {
        initialized = true;
      }
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }
  }
}
