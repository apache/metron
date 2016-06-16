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

package org.apache.metron.parsers.bolt;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.common.interfaces.MessageWriter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class WriterBoltTest extends BaseBoltTest{
  @Mock
  protected TopologyContext topologyContext;

  @Mock
  protected OutputCollector outputCollector;

  @Mock
  private MessageWriter<JSONObject> writer;

  @Mock
  private BulkMessageWriter<JSONObject> batchWriter;

  private ParserConfigurations getConfigurations(int batchSize) {
    return new ParserConfigurations() {
          @Override
          public SensorParserConfig getSensorParserConfig(String sensorType) {
            return new SensorParserConfig() {
              @Override
              public Map<String, Object> getParserConfig() {
                return new HashMap<String, Object>() {{
                  put(ParserWriterConfiguration.BATCH_CONF, batchSize);
                }};
              }
            };
          }
        };
  }
  @Test
  public void testBatchHappyPath() throws Exception {
    ParserConfigurations configurations = getConfigurations(5);
    String sensorType = "test";
    List<Tuple> tuples = new ArrayList<>();
    for(int i = 0;i < 5;++i) {
      Tuple t = mock(Tuple.class);
      when(t.getValueByField(eq("message"))).thenReturn(new JSONObject());
      tuples.add(t);
    }
    WriterBolt bolt = new WriterBolt(new WriterHandler(batchWriter), configurations, sensorType);
    bolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(batchWriter, times(1)).init(any(), any());
    for(int i = 0;i < 4;++i) {
      Tuple t = tuples.get(i);
      bolt.execute(t);
      verify(outputCollector, times(0)).ack(t);
      verify(batchWriter, times(0)).write(eq(sensorType), any(), any(), any());
    }
    bolt.execute(tuples.get(4));
    for(Tuple t : tuples) {
      verify(outputCollector, times(1)).ack(t);
    }
    verify(batchWriter, times(1)).write(eq(sensorType), any(), any(), any());
    verify(outputCollector, times(0)).reportError(any());
    verify(outputCollector, times(0)).fail(any());
  }
  @Test
  public void testNonBatchHappyPath() throws Exception {
    ParserConfigurations configurations = getConfigurations(1);
    String sensorType = "test";
    Tuple t = mock(Tuple.class);
    when(t.getValueByField(eq("message"))).thenReturn(new JSONObject());
    WriterBolt bolt = new WriterBolt(new WriterHandler(writer), configurations, sensorType);
    bolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(writer, times(1)).init();
    bolt.execute(t);
    verify(outputCollector, times(1)).ack(t);
    verify(writer, times(1)).write(eq(sensorType), any(), any(), any());
    verify(outputCollector, times(0)).reportError(any());
    verify(outputCollector, times(0)).fail(any());
  }
  @Test
  public void testNonBatchErrorPath() throws Exception {
    ParserConfigurations configurations = getConfigurations(1);
    String sensorType = "test";
    Tuple t = mock(Tuple.class);
    when(t.getValueByField(eq("message"))).thenThrow(new IllegalStateException());
    WriterBolt bolt = new WriterBolt(new WriterHandler(writer), configurations, sensorType);
    bolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(writer, times(1)).init();
    bolt.execute(t);
    verify(outputCollector, times(1)).ack(t);
    verify(writer, times(0)).write(eq(sensorType), any(), any(), any());
    verify(outputCollector, times(1)).reportError(any());
    verify(outputCollector, times(0)).fail(any());
  }
  @Test
  public void testNonBatchErrorPathErrorInWrite() throws Exception {
    ParserConfigurations configurations = getConfigurations(1);
    String sensorType = "test";
    Tuple t = mock(Tuple.class);
    when(t.getValueByField(eq("message"))).thenReturn(new JSONObject());
    WriterBolt bolt = new WriterBolt(new WriterHandler(writer), configurations, sensorType);
    bolt.prepare(new HashMap(), topologyContext, outputCollector);
    doThrow(new Exception()).when(writer).write(any(), any(), any(), any());
    verify(writer, times(1)).init();
    bolt.execute(t);
    verify(outputCollector, times(1)).ack(t);
    verify(writer, times(1)).write(eq(sensorType), any(), any(), any());
    verify(outputCollector, times(1)).reportError(any());
    verify(outputCollector, times(0)).fail(any());
  }
  @Test
  public void testBatchErrorPath() throws Exception {
    ParserConfigurations configurations = getConfigurations(5);
    String sensorType = "test";
    List<Tuple> tuples = new ArrayList<>();
    for(int i = 0;i < 4;++i) {
      Tuple t = mock(Tuple.class);
      when(t.getValueByField(eq("message"))).thenReturn(new JSONObject());
      tuples.add(t);
    }
    Tuple errorTuple = mock(Tuple.class);
    Tuple goodTuple = mock(Tuple.class);
    when(goodTuple.getValueByField(eq("message"))).thenReturn(new JSONObject());
    when(errorTuple.getValueByField(eq("message"))).thenThrow(new IllegalStateException());

    WriterBolt bolt = new WriterBolt(new WriterHandler(batchWriter), configurations, sensorType);
    bolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(batchWriter, times(1)).init(any(), any());
    for(int i = 0;i < 4;++i) {
      Tuple t = tuples.get(i);
      bolt.execute(t);
      verify(outputCollector, times(0)).ack(t);
      verify(batchWriter, times(0)).write(eq(sensorType), any(), any(), any());
    }
    bolt.execute(errorTuple);
    for(Tuple t : tuples) {
      verify(outputCollector, times(0)).ack(t);
    }
    bolt.execute(goodTuple);
    for(Tuple t : tuples) {
      verify(outputCollector, times(1)).ack(t);
    }
    verify(outputCollector, times(1)).ack(goodTuple);
    verify(batchWriter, times(1)).write(eq(sensorType), any(), any(), any());
    verify(outputCollector, times(1)).reportError(any());
    verify(outputCollector, times(0)).fail(any());
  }

  @Test
  public void testBatchErrorPathExceptionInWrite() throws Exception {
    ParserConfigurations configurations = getConfigurations(5);
    String sensorType = "test";
    List<Tuple> tuples = new ArrayList<>();
    for(int i = 0;i < 4;++i) {
      Tuple t = mock(Tuple.class);
      when(t.getValueByField(eq("message"))).thenReturn(new JSONObject());
      tuples.add(t);
    }
    Tuple goodTuple = mock(Tuple.class);
    when(goodTuple.getValueByField(eq("message"))).thenReturn(new JSONObject());

    WriterBolt bolt = new WriterBolt(new WriterHandler(batchWriter), configurations, sensorType);
    bolt.prepare(new HashMap(), topologyContext, outputCollector);
    doThrow(new Exception()).when(batchWriter).write(any(), any(), any(), any());
    verify(batchWriter, times(1)).init(any(), any());
    for(int i = 0;i < 4;++i) {
      Tuple t = tuples.get(i);
      bolt.execute(t);
      verify(outputCollector, times(0)).ack(t);
      verify(batchWriter, times(0)).write(eq(sensorType), any(), any(), any());
    }
    bolt.execute(goodTuple);
    for(Tuple t : tuples) {
      verify(outputCollector, times(1)).ack(t);
    }
    verify(batchWriter, times(1)).write(eq(sensorType), any(), any(), any());
    verify(outputCollector, times(1)).ack(goodTuple);
    verify(outputCollector, times(1)).reportError(any());
    verify(outputCollector, times(0)).fail(any());
  }
}
