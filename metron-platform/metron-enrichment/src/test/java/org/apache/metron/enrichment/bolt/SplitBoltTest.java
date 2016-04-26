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
package org.apache.metron.enrichment.bolt;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import junit.framework.Assert;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.apache.metron.common.configuration.Configurations;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SplitBoltTest extends BaseEnrichmentBoltTest {

  public class StandAloneSplitBolt extends SplitBolt<JSONObject> {

    public StandAloneSplitBolt(String zookeeperUrl) {
      super(zookeeperUrl);
    }


    @Override
    public void prepare(Map map, TopologyContext topologyContext) {

    }

    @Override
    public Set<String> getStreamIds() {
      return streamIds;
    }

    @Override
    public String getKey(Tuple tuple, JSONObject message) {
      return key;
    }

    @Override
    public JSONObject generateMessage(Tuple tuple) {
      return sampleMessage;
    }

    @Override
    public Map<String, JSONObject> splitMessage(JSONObject message) {
      return null;
    }

    @Override
    public void declareOther(OutputFieldsDeclarer declarer) {

    }

    @Override
    public void emitOther(Tuple tuple, JSONObject message) {

    }
  }

  @Test
  public void test() {
    StandAloneSplitBolt splitBolt = spy(new StandAloneSplitBolt("zookeeperUrl"));
    splitBolt.setCuratorFramework(client);
    splitBolt.setTreeCache(cache);
    doCallRealMethod().when(splitBolt).reloadCallback(anyString(), any(Configurations.Type.class));
    splitBolt.prepare(new HashMap(), topologyContext, outputCollector);
    splitBolt.declareOutputFields(declarer);
    verify(declarer, times(1)).declareStream(eq("message"), argThat(new FieldsMatcher("key", "message")));
    for(String streamId: streamIds) {
      verify(declarer, times(1)).declareStream(eq(streamId), argThat(new FieldsMatcher("key", "message")));
    }
    verify(declarer, times(1)).declareStream(eq("error"), argThat(new FieldsMatcher("message")));

    JSONObject sampleMessage = splitBolt.generateMessage(tuple);
    Map<String, JSONObject> streamMessageMap = new HashMap<>();
    streamMessageMap.put("geo", geoMessage);
    streamMessageMap.put("host", hostMessage);
    streamMessageMap.put("hbaseEnrichment", hbaseEnrichmentMessage);
    doReturn(streamMessageMap).when(splitBolt).splitMessage(sampleMessage);
    splitBolt.execute(tuple);
    verify(outputCollector, times(1)).emit(eq("message"), any(tuple.getClass()), eq(new Values(key, sampleMessage)));
    verify(outputCollector, times(1)).emit(eq("geo"), eq(new Values(key, geoMessage)));
    verify(outputCollector, times(1)).emit(eq("host"), eq(new Values(key, hostMessage)));
    verify(outputCollector, times(1)).emit(eq("hbaseEnrichment"), eq(new Values(key, hbaseEnrichmentMessage)));
    verify(outputCollector, times(1)).ack(tuple);
    streamMessageMap = new HashMap<>();
    streamMessageMap.put("host", null);
    doReturn(streamMessageMap).when(splitBolt).splitMessage(sampleMessage);
    try {
      splitBolt.execute(tuple);
      Assert.fail("An exception should be thrown when splitMessage produces a null value for a stream");
    }catch (IllegalArgumentException e) {}
  }


}
