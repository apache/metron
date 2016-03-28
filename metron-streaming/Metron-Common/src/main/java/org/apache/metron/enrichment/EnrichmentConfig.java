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

package org.apache.metron.enrichment;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.Constants;
import org.apache.metron.domain.SourceConfig;
import org.apache.metron.domain.SourceConfigUtils;

import java.util.*;

public class EnrichmentConfig {
  public static enum Type {
     THREAT_INTEL
    ,ENRICHMENT
  }
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
    CuratorFramework client = SourceConfigUtils.getClient(getZkQuorum());
    try {
      updateSensorConfigs(client, sensorToFieldList);
    }
    finally {
      client.close();
    }
  }
  public static void updateSensorConfigs(CuratorFramework client, Map<String, FieldList> sensorToFieldList) throws Exception {
    Map<String, SourceConfig> sourceConfigsChanged = new HashMap<>();
    for (Map.Entry<String, FieldList> kv : sensorToFieldList.entrySet()) {
      SourceConfig config = sourceConfigsChanged.get(kv.getKey());
      if(config == null) {
        config = SourceConfigUtils.readConfigFromZookeeper(client, kv.getKey());
      }
      Map<String, List<String> > fieldMap = null;
      Map<String, List<String> > fieldToTypeMap = null;
      List<String> fieldList = null;
      if(kv.getValue().type == Type.THREAT_INTEL) {
        fieldMap = config.getThreatIntelFieldMap();
        if(fieldMap!= null) {
          fieldList = fieldMap.get(Constants.SIMPLE_HBASE_THREAT_INTEL);
        }
        fieldToTypeMap = config.getFieldToThreatIntelTypeMap();
        if(fieldToTypeMap == null) {
          fieldToTypeMap = new HashMap<>();
          config.setFieldToThreatIntelTypeMap(fieldToTypeMap);
        }
      }
      else if(kv.getValue().type == Type.ENRICHMENT) {
        fieldMap = config.getEnrichmentFieldMap();
        if(fieldMap!= null) {
          fieldList = fieldMap.get(Constants.SIMPLE_HBASE_ENRICHMENT);
        }
        fieldToTypeMap = config.getFieldToEnrichmentTypeMap();
        if(fieldToTypeMap == null) {
          fieldToTypeMap = new HashMap<>();
          config.setFieldToEnrichmentTypeMap(fieldToTypeMap);
        }
      }
      if(fieldToTypeMap == null || fieldList == null) {
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
          fieldList.addAll(additionalFields);
          sourceConfigsChanged.put(kv.getKey(), config);
        }
      }
      //Add the additional enrichment types to the mapping between the fields
      {
        for(Map.Entry<String, List<String>> fieldToType : fieldToTypeMap.entrySet()) {
          String field = fieldToType.getKey();
          List<String> additionalTypes = fieldToTypeMap.get(field);
          if(additionalTypes != null) {
            Set<String> enrichmentTypes = new HashSet<>(fieldToType.getValue());
            for(String additionalType : additionalTypes) {
              if(!enrichmentTypes.contains(additionalType)) {
                fieldToType.getValue().add(additionalType);
              }
            }
          }
        }
      }
    }
    for(Map.Entry<String, SourceConfig> kv : sourceConfigsChanged.entrySet()) {
      SourceConfigUtils.writeToZookeeper(client, kv.getKey(), kv.getValue().toJSON().getBytes());
    }
  }

}
