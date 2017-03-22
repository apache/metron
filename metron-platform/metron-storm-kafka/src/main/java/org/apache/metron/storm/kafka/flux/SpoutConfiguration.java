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
package org.apache.metron.storm.kafka.flux;

import org.apache.metron.common.utils.ConversionUtils;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public enum SpoutConfiguration {
  POLL_TIMEOUT_MS("spout.pollTimeoutMs"
                 , container -> container.builder.setPollTimeoutMs(ConversionUtils.convert(container.value, Long.class))
                 )
  ,FIRST_POLL_OFFSET_STRATEGY("spout.firstPollOffsetStrategy"
                 , container -> container.builder.setFirstPollOffsetStrategy(KafkaSpoutConfig.FirstPollOffsetStrategy.valueOf(container.value.toString()))
                 )
  ,MAX_RETRIES("spout.maxRetries"
                 , container -> container.builder.setMaxRetries(ConversionUtils.convert(container.value, Integer.class))
                 )
  ,MAX_UNCOMMITTED_OFFSETS("spout.maxUncommittedOffsets"
                 , container -> container.builder.setMaxUncommittedOffsets(ConversionUtils.convert(container.value, Integer.class))
                 )
  ,OFFSET_COMMIT_PERIOD_MS("spout.offsetCommitPeriodMs"
                 , container -> container.builder.setOffsetCommitPeriodMs(ConversionUtils.convert(container.value, Long.class))
                 )
  ;
  private static class Container {
    Map<String, Object> config;
    KafkaSpoutConfig.Builder builder;
    Object value;
    public Container(Map<String, Object> config, KafkaSpoutConfig.Builder builder, Object value) {
      this.config = config;
      this.builder = builder;
      this.value = value;
    }
  }
  Consumer<Container> consumer;
  public String key;
  SpoutConfiguration(String key, Consumer<Container> consumer) {
    this.consumer = consumer;
    this.key = key;
  }

  public static Map<String, Object> separate(Map<String, Object> config) {
    Map<String, Object> ret = new HashMap<>();
    for(SpoutConfiguration spoutConfig : SpoutConfiguration.values()) {
      if(config.containsKey(spoutConfig.key)) {
        Object val = config.get(spoutConfig.key);
        config.remove(spoutConfig.key);
        ret.put(spoutConfig.key, val);
      }
    }
    return ret;
  }

  public static <K, V> KafkaSpoutConfig.Builder configure( KafkaSpoutConfig.Builder<K, V> builder
                                                         , Map<String, Object> config
                                                         )
  {
    for(SpoutConfiguration spoutConfig : SpoutConfiguration.values()) {
      if(config.containsKey(spoutConfig.key)) {
        Container container = new Container(config, builder, config.get(spoutConfig.key));
        spoutConfig.consumer.accept(container);
      }
    }
    return builder;
  }

  public static List<String> allOptions() {
    List<String> ret = new ArrayList<>();
    for(SpoutConfiguration spoutConfig : SpoutConfiguration.values()) {
      ret.add(spoutConfig.key);
    }
    ret.add(KafkaSpoutConfig.Consumer.GROUP_ID);
    ret.add(KafkaSpoutConfig.Consumer.AUTO_COMMIT_INTERVAL_MS);
    ret.add(KafkaSpoutConfig.Consumer.ENABLE_AUTO_COMMIT);
    return ret;
  }
}
