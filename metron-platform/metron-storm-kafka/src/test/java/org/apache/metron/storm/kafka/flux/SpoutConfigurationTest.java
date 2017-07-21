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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class SpoutConfigurationTest {

  @Test
  public void testSeparation() {
    Map<String, Object>  config = new HashMap<String, Object>() {{
      put(SpoutConfiguration.FIRST_POLL_OFFSET_STRATEGY.key, "UNCOMMITTED_EARLIEST");
      put(SpoutConfiguration.OFFSET_COMMIT_PERIOD_MS.key, "1000");
      put("group.id", "foobar");
    }};
    Map<String, Object> spoutConfig = SpoutConfiguration.separate(config);
    Assert.assertTrue(spoutConfig.containsKey(SpoutConfiguration.FIRST_POLL_OFFSET_STRATEGY.key));
    Assert.assertEquals(spoutConfig.get(SpoutConfiguration.FIRST_POLL_OFFSET_STRATEGY.key), "UNCOMMITTED_EARLIEST");
    Assert.assertTrue(spoutConfig.containsKey(SpoutConfiguration.OFFSET_COMMIT_PERIOD_MS.key));
    Assert.assertEquals(spoutConfig.get(SpoutConfiguration.OFFSET_COMMIT_PERIOD_MS.key), "1000");
    Assert.assertEquals(2, spoutConfig.size());
    Assert.assertEquals(1, config.size());
    Assert.assertEquals(config.get("group.id"), "foobar");
  }

  @Test
  public void testBuilderCreation() {
    Map<String, Object>  config = new HashMap<String, Object>() {{
      put(SpoutConfiguration.OFFSET_COMMIT_PERIOD_MS.key, "1000");
      put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "foo:1234");
      put("group.id", "foobar");
    }};
    Map<String, Object> spoutConfig = SpoutConfiguration.separate(config);
    KafkaSpoutConfig.Builder<Object, Object> builder = new SimpleStormKafkaBuilder(config, "topic", null);
    SpoutConfiguration.configure(builder, spoutConfig);
    KafkaSpoutConfig c = builder.build();
    Assert.assertEquals(1000, c.getOffsetsCommitPeriodMs() );
  }

}
