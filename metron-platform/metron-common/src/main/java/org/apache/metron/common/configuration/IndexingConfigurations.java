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
package org.apache.metron.common.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.utils.JSONUtils;

public class IndexingConfigurations extends Configurations {
  public static final String BATCH_SIZE_CONF = "batchSize";
  public static final String BATCH_TIMEOUT_CONF = "batchTimeout";
  public static final String ENABLED_CONF = "enabled";
  public static final String INDEX_CONF = "index";
  public static final String OUTPUT_PATH_FUNCTION_CONF = "outputPathFunction";
  public static final String FIELD_NAME_CONVERTER_CONF = "fieldNameConverter";

  public Map<String, Object> getSensorIndexingConfig(String sensorType, boolean emptyMapOnNonExistent) {
    Map<String, Object> ret = (Map<String, Object>) getConfigurations().get(getKey(sensorType));
    if(ret == null) {
      return emptyMapOnNonExistent?new HashMap<>():null;
    }
    else {
      return ret;
    }
  }

  public Map<String, Object> getSensorIndexingConfig(String sensorType) {
    return getSensorIndexingConfig(sensorType, true);
  }

  public List<String> getTypes() {
    List<String> ret = new ArrayList<>();
    for(String keyedSensor : getConfigurations().keySet()) {
      if(!keyedSensor.isEmpty() && keyedSensor.startsWith(ConfigurationType.INDEXING.getTypeName())) {
        ret.add(keyedSensor.substring(ConfigurationType.INDEXING.getTypeName().length() + 1));
      }
    }
    return ret;
  }

  public void delete(String sensorType) {
    getConfigurations().remove(getKey(sensorType));
  }

  public Map<String, Object> getSensorIndexingConfig(String sensorType, String writerName) {
    String key = getKey(sensorType);
    Map<String, Object> ret = (Map<String, Object>) getConfigurations().get(key);
    if(ret == null) {
      return new HashMap();
    }
    else {
      Map<String, Object> writerConfig = (Map<String, Object>)ret.get(writerName);
      return writerConfig != null?writerConfig:new HashMap<>();
    }
  }

  public void updateSensorIndexingConfig(String sensorType, byte[] data) throws IOException {
    updateSensorIndexingConfig(sensorType, new ByteArrayInputStream(data));
  }

  public void updateSensorIndexingConfig(String sensorType, InputStream io) throws IOException {
    Map<String, Object> sensorIndexingConfig = JSONUtils.INSTANCE.load(io, JSONUtils.MAP_SUPPLIER);
    updateSensorIndexingConfig(sensorType, sensorIndexingConfig);
  }

  public void updateSensorIndexingConfig(String sensorType, Map<String, Object> sensorIndexingConfig) {
    getConfigurations().put(getKey(sensorType), sensorIndexingConfig);
  }

  public static String getKey(String sensorType) {
    return ConfigurationType.INDEXING.getTypeName() + "." + sensorType;
  }

  public boolean isDefault(String sensorName, String writerName) {
    Map<String, Object> ret = (Map<String, Object>) getConfigurations().get(getKey(sensorName));
    if(ret == null) {
      return true;
    }
    else {
      Map<String, Object> writerConfig = (Map<String, Object>)ret.get(writerName);
      return writerConfig != null?false:true;
    }
  }

  public int getBatchSize(String sensorName, String writerName ) {
    return getBatchSize(getSensorIndexingConfig(sensorName, writerName));
  }

  public int getBatchTimeout(String sensorName, String writerName ) {
    return getBatchTimeout(getSensorIndexingConfig(sensorName, writerName));
  }

  /**
   * Returns all configured values of batchTimeout, for all configured sensors,
   * but only for the specific writer identified by {@code writerName}.  So, if it is
   * an hdfs writer, it will return the batchTimeouts for hdfs writers for all the sensors.
   * The goal is to return to a {@link org.apache.metron.common.bolt.ConfiguredBolt}
   * the set of all and only batchTimeouts relevant to that ConfiguredBolt.
   *
   * @param writerName
   * @return list of integer batchTimeouts, one per configured sensor
   */
  public List<Integer> getAllConfiguredTimeouts(String writerName) {
    // The configuration infrastructure was not designed to enumerate sensors, so we synthesize.
    // Since getKey is in this same class, we know we can pass it a null string to get the key prefix
    // for all sensor types within this capability.  We then enumerate all keys in configurations.keySet
    // and select those that match the key prefix, as being sensor keys.  The suffix substring of
    // each such key is used as a sensor name to query the batchTimeout settings, if any.
    String keyPrefixString = getKey("");
    int prefixStringLength = keyPrefixString.length();
    List<Integer> configuredBatchTimeouts = new ArrayList<>();
    for (String sensorKeyString : getConfigurations().keySet()) {
      if (sensorKeyString.startsWith(keyPrefixString)) {
        String configuredSensorName = sensorKeyString.substring(prefixStringLength);
        configuredBatchTimeouts.add(getBatchTimeout(configuredSensorName, writerName));
      }
    }
    return configuredBatchTimeouts;
  }

  public String getIndex(String sensorName, String writerName) {
    return getIndex(getSensorIndexingConfig(sensorName, writerName), sensorName);
  }

  public boolean isEnabled(String sensorName, String writerName) {
    return isEnabled(getSensorIndexingConfig(sensorName, writerName));
  }

  public String getOutputPathFunction(String sensorName, String writerName) {
    return getOutputPathFunction(getSensorIndexingConfig(sensorName, writerName), sensorName);
  }

  public String getFieldNameConverter(String sensorName, String writerName) {
    return getFieldNameConverter(getSensorIndexingConfig(sensorName, writerName), sensorName);
  }

  public static boolean isEnabled(Map<String, Object> conf) {
    return getAs( ENABLED_CONF
                 ,conf
                , true
                , Boolean.class
                );
  }

  public static int getBatchSize(Map<String, Object> conf) {
    return getAs( BATCH_SIZE_CONF
                 ,conf
                , 1
                , Integer.class
                );
  }

  public static int getBatchTimeout(Map<String, Object> conf) {
    return getAs( BATCH_TIMEOUT_CONF
                 ,conf
                , 0
                , Integer.class
                );
  }

  public static String getIndex(Map<String, Object> conf, String sensorName) {
    return getAs( INDEX_CONF
                 ,conf
                , sensorName
                , String.class
                );
  }

  public static String getOutputPathFunction(Map<String, Object> conf, String sensorName) {
    return getAs(OUTPUT_PATH_FUNCTION_CONF
            ,conf
            , ""
            , String.class
    );
  }

  public static String getFieldNameConverter(Map<String, Object> conf, String sensorName) {
    return getAs(FIELD_NAME_CONVERTER_CONF, conf, "", String.class);
  }

  public static Map<String, Object> setEnabled(Map<String, Object> conf, boolean enabled) {
    Map<String, Object> ret = conf == null?new HashMap<>():conf;
    ret.put(ENABLED_CONF, enabled);
    return ret;
  }
  public static Map<String, Object> setBatchSize(Map<String, Object> conf, int batchSize) {
    Map<String, Object> ret = conf == null?new HashMap<>():conf;
    ret.put(BATCH_SIZE_CONF, batchSize);
    return ret;
  }

  public static Map<String, Object> setBatchTimeout(Map<String, Object> conf, int batchTimeout) {
    Map<String, Object> ret = conf == null?new HashMap<>():conf;
    ret.put(BATCH_TIMEOUT_CONF, batchTimeout);
    return ret;
  }

  public static Map<String, Object> setIndex(Map<String, Object> conf, String index) {
    Map<String, Object> ret = conf == null?new HashMap<>():conf;
    ret.put(INDEX_CONF, index);
    return ret;
  }

  public static Map<String, Object> setFieldNameConverter(Map<String, Object> conf, String index) {
    Map<String, Object> ret = conf == null ? new HashMap<>(): conf;
    ret.put(FIELD_NAME_CONVERTER_CONF, index);
    return ret;
  }

}
