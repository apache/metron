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
    List<String> fieldList;

    public Type getType() {
      return type;
    }

    public void setType(Type type) {
      this.type = type;
    }

    public List<String> getFieldList() {
      return fieldList;
    }

    public void setFieldList(List<String> fieldList) {
      this.fieldList = fieldList;
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

  public void updateSensorConfigs() throws Exception {
    CuratorFramework client = SourceConfigUtils.getClient(getZkQuorum());
    try {
      Map<String, SourceConfig> sourceConfigsChanged = new HashMap<>();
      for (Map.Entry<String, FieldList> kv : sensorToFieldList.entrySet()) {
        SourceConfig config = sourceConfigsChanged.get(kv.getKey());
        if(config == null) {
          config = SourceConfigUtils.readConfigFromZookeeper(client, kv.getKey());
        }
        Map<String, List<String> > fieldToTypeMap = null;
        List<String> fieldList = null;
        if(kv.getValue().type == Type.THREAT_INTEL) {
          fieldToTypeMap = config.getFieldToThreatIntelTypeMap();
          if(fieldToTypeMap != null) {
            fieldList = fieldToTypeMap.get(Constants.SIMPLE_HBASE_THREAT_INTEL);
          }
        }
        else if(kv.getValue().type == Type.ENRICHMENT) {
          fieldToTypeMap = config.getFieldToEnrichmentTypeMap();
          if(fieldToTypeMap != null) {
            fieldList = fieldToTypeMap.get(Constants.SIMPLE_HBASE_ENRICHMENT);
          }
        }
        if(fieldToTypeMap == null || fieldList == null) {
          continue;
        }
        HashSet<String> fieldSet = new HashSet<>(fieldList);
        List<String> additionalFields = new ArrayList<>();
        for(String field : kv.getValue().getFieldList()) {
          if (!fieldSet.contains(field)) {
            additionalFields.add(field);
          }
        }
        //adding only the ones that we don't already have.
        if(additionalFields.size() > 0) {
          fieldList.addAll(additionalFields);
          sourceConfigsChanged.put(kv.getKey(), config);
        }
      }
      for(Map.Entry<String, SourceConfig> kv : sourceConfigsChanged.entrySet()) {
        SourceConfigUtils.writeToZookeeper(client, kv.getKey(), kv.getValue().toJSON().getBytes());
      }
    }
    finally {
      client.close();
    }
  }

}
