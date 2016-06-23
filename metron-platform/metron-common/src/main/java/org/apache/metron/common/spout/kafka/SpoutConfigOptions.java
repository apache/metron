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

package org.apache.metron.common.spout.kafka;

import com.google.common.base.Joiner;
import org.apache.metron.common.utils.ConversionUtils;
import storm.kafka.SpoutConfig;

import java.util.EnumMap;
import java.util.Map;

public enum SpoutConfigOptions implements SpoutConfigFunction {
  retryDelayMaxMs( (config, val) -> config.retryDelayMaxMs = convertVal(val, Long.class) ),
  retryDelayMultiplier ( (config, val) -> config.retryDelayMultiplier = convertVal(val, Double.class)),
  retryInitialDelayMs( (config, val) -> config.retryInitialDelayMs = convertVal(val, Long.class)),
  stateUpdateIntervalMs( (config, val) -> config.stateUpdateIntervalMs = convertVal(val, Long.class)),
  bufferSizeBytes( (config, val) -> config.bufferSizeBytes = convertVal(val, Integer.class)),
  fetchMaxWait( (config, val) -> config.fetchMaxWait = convertVal(val, Integer.class)),
  fetchSizeBytes( (config, val) -> config.fetchSizeBytes= convertVal(val, Integer.class)),
  maxOffsetBehind( (config, val) -> config.maxOffsetBehind = convertVal(val, Long.class)),
  metricsTimeBucketSizeInSecs( (config, val) -> config.metricsTimeBucketSizeInSecs = convertVal(val, Integer.class)),
  socketTimeoutMs( (config, val) -> config.socketTimeoutMs = convertVal(val, Integer.class)),
  ;

  SpoutConfigFunction  spoutConfigFunc;
  SpoutConfigOptions(SpoutConfigFunction spoutConfigFunc) {
    this.spoutConfigFunc = spoutConfigFunc;
  }

  @Override
  public void configure(SpoutConfig config, Object val) {
    spoutConfigFunc.configure(config, val);
  }

  public static SpoutConfig configure(SpoutConfig config, EnumMap<SpoutConfigOptions, Object> configs) {
    if(configs != null) {
      for(Map.Entry<SpoutConfigOptions, Object> kv : configs.entrySet()) {
        kv.getKey().configure(config, kv.getValue());
      }
    }
    return config;
  }

  public static EnumMap<SpoutConfigOptions, Object> coerceMap(Map<String, Object> map) {
    EnumMap<SpoutConfigOptions, Object> ret = new EnumMap<>(SpoutConfigOptions.class);
    for(Map.Entry<String, Object> kv : map.entrySet()) {
      try {
        ret.put(SpoutConfigOptions.valueOf(kv.getKey()), kv.getValue());
      }
      catch(Exception ex) {
        String possibleOptions = Joiner.on(",").join(SpoutConfigOptions.values());
        throw new IllegalArgumentException("Configuration keys for spout config must be one of: " + possibleOptions, ex);
      }
    }
    return ret;
  }
  private static <EXPECTED_T> EXPECTED_T convertVal(Object val, Class<EXPECTED_T> clazz) {
    Object ret = ConversionUtils.convert(val, clazz);
    if(ret == null) {
      throw new IllegalArgumentException("Unable to convert " + val + " to expected type " + clazz.getCanonicalName());
    }
    return clazz.cast(ret);
  }
}
