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

package org.apache.metron.common.configuration.enrichment;

import com.google.common.base.Joiner;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SensorEnrichmentUpdateConfig {

  protected static final Logger _LOG = LoggerFactory.getLogger(SensorEnrichmentUpdateConfig.class);
  public static class FieldList {
    Type type;
    Map<String, List<String>> fieldToEnrichmentTypes;

    public Type getType() {
      return type;
    }

    public void setType(Type type) {
      this.type = type;
    }

    public Map<String, List<String>> getFieldToEnrichmentTypes() {
      return fieldToEnrichmentTypes;
    }

    public void setFieldToEnrichmentTypes(Map<String, List<String>> fieldToEnrichmentTypes) {
      this.fieldToEnrichmentTypes = fieldToEnrichmentTypes;
    }
  }
  public String zkQuorum;
  public Map<String, FieldList> sensorToFieldList;

  public String getZkQuorum() {
    return zkQuorum;
  }

  public void setZkQuorum(String zkQuorum) {
    this.zkQuorum = zkQuorum;
  }

  public Map<String, FieldList> getSensorToFieldList() {
    return sensorToFieldList;
  }

  public void setSensorToFieldList(Map<String, FieldList> sensorToFieldList) {
    this.sensorToFieldList = sensorToFieldList;
  }

  public void updateSensorConfigs( ) throws Exception {
    CuratorFramework client = ConfigurationsUtils.getClient(getZkQuorum());
    try {
      client.start();
      updateSensorConfigs(new ZKSourceConfigHandler(client), sensorToFieldList);
    }
    finally {
      client.close();
    }
  }

  public static interface SourceConfigHandler {
    SensorEnrichmentConfig readConfig(String sensor) throws Exception;
    void persistConfig(String sensor, SensorEnrichmentConfig config) throws Exception;
  }

  public static class ZKSourceConfigHandler implements SourceConfigHandler {
    CuratorFramework client;
    public ZKSourceConfigHandler(CuratorFramework client) {
      this.client = client;
    }
    @Override
    public SensorEnrichmentConfig readConfig(String sensor) throws Exception {
      SensorEnrichmentConfig sensorEnrichmentConfig = new SensorEnrichmentConfig();
      try {
        sensorEnrichmentConfig = SensorEnrichmentConfig.fromBytes(ConfigurationsUtils.readSensorEnrichmentConfigBytesFromZookeeper(sensor, client));
      }catch (KeeperException.NoNodeException e) {
        sensorEnrichmentConfig.setIndex(sensor);
        sensorEnrichmentConfig.setBatchSize(1);
      }
      return sensorEnrichmentConfig;
    }

    @Override
    public void persistConfig(String sensor, SensorEnrichmentConfig config) throws Exception {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensor, config.toJSON().getBytes(), client);
    }
  }

  public static void updateSensorConfigs( SourceConfigHandler scHandler
                                        , Map<String, FieldList> sensorToFieldList
                                        ) throws Exception
  {
    Map<String, SensorEnrichmentConfig> sourceConfigsChanged = new HashMap<>();
    for (Map.Entry<String, FieldList> kv : sensorToFieldList.entrySet()) {
      SensorEnrichmentConfig config = findConfigBySensorType(scHandler, sourceConfigsChanged, kv.getKey());
      Map<String, List<String> > fieldMap = null;
      Map<String, List<String> > fieldToTypeMap = null;
      List<String> fieldList = null;
      if(kv.getValue().type == Type.THREAT_INTEL) {
        fieldMap = config.getThreatIntel().getFieldMap();
        if(fieldMap!= null) {
          fieldList = fieldMap.get(Constants.SIMPLE_HBASE_THREAT_INTEL);
        } else {
          fieldMap = new HashMap<>();
        }
        if(fieldList == null) {
          fieldList = new ArrayList<>();
          fieldMap.put(Constants.SIMPLE_HBASE_THREAT_INTEL, fieldList);
        }
        fieldToTypeMap = config.getThreatIntel().getFieldToTypeMap();
        if(fieldToTypeMap == null) {
          fieldToTypeMap = new HashMap<>();
          config.getThreatIntel().setFieldToTypeMap(fieldToTypeMap);
        }
      }
      else if(kv.getValue().type == Type.ENRICHMENT) {
        fieldMap = config.getEnrichment().getFieldMap();
        if(fieldMap!= null) {
          fieldList = fieldMap.get(Constants.SIMPLE_HBASE_ENRICHMENT);
        } else {
          fieldMap = new HashMap<>();
        }
        if(fieldList == null) {
          fieldList = new ArrayList<>();
          fieldMap.put(Constants.SIMPLE_HBASE_ENRICHMENT, fieldList);
        }
        fieldToTypeMap = config.getEnrichment().getFieldToTypeMap();
        if(fieldToTypeMap == null) {
          fieldToTypeMap = new HashMap<>();
          config.getEnrichment().setFieldToTypeMap(fieldToTypeMap);
        }
      }
      if(fieldToTypeMap == null  || fieldMap == null) {
        _LOG.debug("fieldToTypeMap is null or fieldMap is null, so skipping");
        continue;
      }
      //Add the additional fields to the field list associated with the hbase adapter
      {
        HashSet<String> fieldSet = new HashSet<>(fieldList);
        List<String> additionalFields = new ArrayList<>();
        for (String field : kv.getValue().getFieldToEnrichmentTypes().keySet()) {
          if (!fieldSet.contains(field)) {
            additionalFields.add(field);
          }
        }
        //adding only the ones that we don't already have to the field list
        if (additionalFields.size() > 0) {
          _LOG.debug("Adding additional fields: " + Joiner.on(',').join(additionalFields));
          fieldList.addAll(additionalFields);
          sourceConfigsChanged.put(kv.getKey(), config);
        }
      }
      //Add the additional enrichment types to the mapping between the fields
      {
        for(Map.Entry<String, List<String>> fieldToType : kv.getValue().getFieldToEnrichmentTypes().entrySet()) {
          String field = fieldToType.getKey();
          final HashSet<String> types = new HashSet<>(fieldToType.getValue());
          int sizeBefore = 0;
          if(fieldToTypeMap.containsKey(field)) {
            List<String> typeList = fieldToTypeMap.get(field);
            sizeBefore = new HashSet<>(typeList).size();
            types.addAll(typeList);
          }
          int sizeAfter = types.size();
          boolean changed = sizeBefore != sizeAfter;
          if(changed) {
            fieldToTypeMap.put(field, new ArrayList<String>() {{
                addAll(types);
              }});
            sourceConfigsChanged.put(kv.getKey(), config);
          }
        }
      }
    }
    for(Map.Entry<String, SensorEnrichmentConfig> kv : sourceConfigsChanged.entrySet()) {
      scHandler.persistConfig(kv.getKey(), kv.getValue());
    }
  }

  private static SensorEnrichmentConfig findConfigBySensorType(SourceConfigHandler scHandler, Map<String, SensorEnrichmentConfig> sourceConfigsChanged, String key) throws Exception {
    SensorEnrichmentConfig config = sourceConfigsChanged.get(key);
    if(config == null) {
      config = scHandler.readConfig(key);
      if(_LOG.isDebugEnabled()) {
        _LOG.debug(config.toJSON());
      }
    }
    return config;
  }

}
