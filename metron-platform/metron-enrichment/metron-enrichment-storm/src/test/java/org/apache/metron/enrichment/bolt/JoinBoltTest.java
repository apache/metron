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

import com.github.benmanes.caffeine.cache.LoadingCache;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.apache.metron.test.error.MetronErrorJSONMatcher;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JoinBoltTest extends BaseEnrichmentBoltTest {

  public class StandAloneJoinBolt extends JoinBolt<JSONObject> {

    public StandAloneJoinBolt(String zookeeperUrl) {
      super(zookeeperUrl);
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext) {

    }

    @Override
    public Set<String> getStreamIds(JSONObject value) {
      HashSet<String> ret = new HashSet<>();
      for(String s : streamIds) {
        ret.add(s + ":");
      }
      ret.add("message:");
      return ret;
    }

    @Override
    public JSONObject joinMessages(Map<String, Tuple> streamMessageMap, MessageGetStrategy messageGetStrategy) {
      return joinedMessage;
    }
  }

  /**
   {
   "joinField": "joinValue"
   }
   */
  @Multiline
  private String joinedMessageString;

  private JSONObject joinedMessage;
  private JoinBolt<JSONObject> joinBolt;

  @Before
  public void parseMessages() {
    JSONParser parser = new JSONParser();
    try {
      joinedMessage = (JSONObject) parser.parse(joinedMessageString);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    joinBolt = new StandAloneJoinBolt("zookeeperUrl");
    joinBolt.setCuratorFramework(client);
    joinBolt.setZKCache(cache);
  }

  @Test
  public void testPrepare() {
    try {
      joinBolt.prepare(new HashMap(), topologyContext, outputCollector);
      fail("Should fail if a maxCacheSize property is not set");
    } catch(IllegalStateException e) {}
    joinBolt.withMaxCacheSize(100);
    try {
      joinBolt.prepare(new HashMap(), topologyContext, outputCollector);
      fail("Should fail if a maxTimeRetain property is not set");
    } catch(IllegalStateException e) {}
    joinBolt.withMaxTimeRetain(10000);
    joinBolt.prepare(new HashMap(), topologyContext, outputCollector);
  }

  @Test
  public void testDeclareOutputFields() {
    joinBolt.declareOutputFields(declarer);
    verify(declarer, times(1)).declareStream(eq("message"), argThat(new FieldsMatcher("key", "message")));
    verify(declarer, times(1)).declareStream(eq("error"), argThat(new FieldsMatcher("message")));
    verifyNoMoreInteractions(declarer);
  }

  @Test
  public void testExecute() {
    joinBolt.withMaxCacheSize(100);
    joinBolt.withMaxTimeRetain(10000);
    joinBolt.prepare(new HashMap(), topologyContext, outputCollector);

    Tuple geoTuple = mock(Tuple.class);
    when(geoTuple.getValueByField("key")).thenReturn(key);
    when(geoTuple.getSourceStreamId()).thenReturn("geo");
    when(geoTuple.getValueByField("message")).thenReturn(geoMessage);
    joinBolt.execute(geoTuple);

    Tuple messageTuple = mock(Tuple.class);
    when(messageTuple.getValueByField("key")).thenReturn(key);
    when(messageTuple.getSourceStreamId()).thenReturn("message");
    when(messageTuple.getValueByField("message")).thenReturn(sampleMessage);
    joinBolt.execute(messageTuple);

    Tuple hostTuple = mock(Tuple.class);
    when(hostTuple.getValueByField("key")).thenReturn(key);
    when(hostTuple.getSourceStreamId()).thenReturn("host");
    when(hostTuple.getValueByField("message")).thenReturn(hostMessage);
    joinBolt.execute(hostTuple);

    Tuple hbaseEnrichmentTuple = mock(Tuple.class);
    when(hbaseEnrichmentTuple.getValueByField("key")).thenReturn(key);
    when(hbaseEnrichmentTuple.getSourceStreamId()).thenReturn("hbaseEnrichment");
    when(hbaseEnrichmentTuple.getValueByField("message")).thenReturn(hbaseEnrichmentMessage);
    joinBolt.execute(hbaseEnrichmentTuple);

    Tuple stellarTuple = mock(Tuple.class);
    when(stellarTuple.getValueByField("key")).thenReturn(key);
    when(stellarTuple.getSourceStreamId()).thenReturn("stellar");
    when(stellarTuple.getValueByField("message")).thenReturn(new JSONObject());
    joinBolt.execute(stellarTuple);

    verify(outputCollector, times(1)).emit(eq("message"), any(tuple.getClass()), eq(new Values(key, joinedMessage)));
    verify(outputCollector, times(1)).ack(messageTuple);

    verifyNoMoreInteractions(outputCollector);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testExecuteShouldReportError() throws ExecutionException {
    joinBolt.withMaxCacheSize(100);
    joinBolt.withMaxTimeRetain(10000);
    joinBolt.prepare(new HashMap(), topologyContext, outputCollector);
    when(tuple.getValueByField("key")).thenReturn(key);
    when(tuple.getValueByField("message")).thenReturn(new JSONObject());
    joinBolt.cache = mock(LoadingCache.class);
    when(joinBolt.cache.get(any())).thenThrow(new RuntimeException(new Exception("join exception")));

    joinBolt.execute(tuple);
    RuntimeException expectedExecutionException = new RuntimeException(new Exception("join exception"));
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.ENRICHMENT_ERROR)
            .withMessage("Joining problem: {}")
            .withThrowable(expectedExecutionException)
            .addRawMessage(new JSONObject());
    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
    verify(outputCollector, times(1)).reportError(any(ExecutionException.class));
    verify(outputCollector, times(1)).ack(eq(tuple));
    verifyNoMoreInteractions(outputCollector);
  }
}
