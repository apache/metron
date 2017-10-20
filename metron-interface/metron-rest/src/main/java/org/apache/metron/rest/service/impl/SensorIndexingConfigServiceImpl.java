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
package org.apache.metron.rest.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

@Service
public class SensorIndexingConfigServiceImpl implements SensorIndexingConfigService {

  private ObjectMapper objectMapper;

  private CuratorFramework client;

  private ConfigurationsCache cache;

  @Autowired
  public SensorIndexingConfigServiceImpl(ObjectMapper objectMapper, CuratorFramework client, ConfigurationsCache cache) {
    this.objectMapper = objectMapper;
    this.client = client;
    this.cache = cache;
  }

  @Override
  public Map<String, Object> save(String name, Map<String, Object> sensorIndexingConfig) throws RestException {
    try {
      ConfigurationsUtils.writeSensorIndexingConfigToZookeeper(name, objectMapper.writeValueAsString(sensorIndexingConfig).getBytes(), client);
    } catch (Exception e) {
      throw new RestException(e);
    }
    return sensorIndexingConfig;
  }

  @Override
  public Map<String, Object> findOne(String name) throws RestException {
    IndexingConfigurations configs = cache.get( IndexingConfigurations.class);
    return configs.getSensorIndexingConfig(name, false);
  }

  @Override
  public Map<String, Map<String, Object>> getAll() throws RestException {
    Map<String, Map<String, Object>> sensorIndexingConfigs = new HashMap<>();
    List<String> sensorNames = getAllTypes();
    for (String name : sensorNames) {
      Map<String, Object> config = findOne(name);
      if(config != null) {
        sensorIndexingConfigs.put(name, config);
      }
    }
    return sensorIndexingConfigs;
  }

  @Override
  public List<String> getAllTypes() throws RestException {
    IndexingConfigurations configs = cache.get( IndexingConfigurations.class);
    return configs.getTypes();
  }

  /**
   * Get a list of index names for a given writer (e.g. elasticsearch, solr, hdfs).
   * This functions in the following way:
   *   * If an index config exists, then the index name will be returned. If unspecified, then the sensor name is used
   *   * If a parser exists and an index does NOT exist, then it will be included.
   *   * If the writer is disabled in the index config, then it will NOT be included.
   * @param writerName The writer name to use
   * @return An iterable of index names
   * @throws RestException
   */
  @Override
  public Iterable<String> getAllIndices(String writerName) throws RestException {
    if(StringUtils.isEmpty(writerName)) {
      return Collections.emptyList();
    }
    IndexingConfigurations indexingConfigs = cache.get( IndexingConfigurations.class);
    ParserConfigurations parserConfigs = cache.get( ParserConfigurations.class);
    Set<String> ret = new HashSet<>();
    for(String sensorName : Iterables.concat(parserConfigs.getTypes(), indexingConfigs.getTypes())) {
      if(indexingConfigs.isEnabled(sensorName, writerName)) {
        String indexName = indexingConfigs.getIndex(sensorName, writerName);
        ret.add(indexName == null ? sensorName : indexName);
      }
    }
    return ret;
  }

  @Override
  public boolean delete(String name) throws RestException {
    try {
        client.delete().forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/" + name);
    } catch (KeeperException.NoNodeException e) {
        return false;
    } catch (Exception e) {
      throw new RestException(e);
    }
    return true;
  }

}
