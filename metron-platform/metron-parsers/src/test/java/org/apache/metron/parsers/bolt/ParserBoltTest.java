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

import org.apache.metron.common.configuration.SensorParserConfig;

import backtype.storm.task.OutputCollector;
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
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParserBoltTest extends BaseBoltTest {

  @Mock
  private MessageParser<JSONObject> parser;

  @Mock
  private MessageWriter<JSONObject> writer;

  @Mock
  private BulkMessageWriter<JSONObject> batchWriter;

  @Mock
  private MessageFilter<JSONObject> filter;

  @Mock
  private Tuple t1;

  @Mock
  private Tuple t2;

  @Mock
  private Tuple t3;

  @Mock
  private Tuple t4;

  @Mock
  private Tuple t5;

  @Test
  public void test() throws Exception {
    String sensorType = "yaf";
    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", sensorType, parser, writer) {
      @Override
      protected ParserConfigurations defaultConfigurations() {
        return new ParserConfigurations() {
          @Override
          public SensorParserConfig getSensorParserConfig(String sensorType) {
            return new SensorParserConfig() {
              @Override
              public Map<String, Object> getParserConfig() {
                return new HashMap<String, Object>() {{
                }};
              }
            };
          }
        };
      }
    };
    parserBolt.setCuratorFramework(client);
    parserBolt.setTreeCache(cache);
    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(parser, times(1)).init();
    verify(writer, times(1)).init();
    byte[] sampleBinary = "some binary message".getBytes();
    JSONParser jsonParser = new JSONParser();
    final JSONObject sampleMessage1 = (JSONObject) jsonParser.parse("{ \"field1\":\"value1\" }");
    final JSONObject sampleMessage2 = (JSONObject) jsonParser.parse("{ \"field2\":\"value2\" }");
    List<JSONObject> messages = new ArrayList<JSONObject>() {{
      add(sampleMessage1);
      add(sampleMessage2);
    }};
    final JSONObject finalMessage1 = (JSONObject) jsonParser.parse("{ \"field1\":\"value1\", \"source.type\":\"" + sensorType + "\" }");
    final JSONObject finalMessage2 = (JSONObject) jsonParser.parse("{ \"field2\":\"value2\", \"source.type\":\"" + sensorType + "\" }");
    when(tuple.getBinary(0)).thenReturn(sampleBinary);
    when(parser.parse(sampleBinary)).thenReturn(messages);
    when(parser.validate(eq(messages.get(0)))).thenReturn(true);
    when(parser.validate(eq(messages.get(1)))).thenReturn(false);
    parserBolt.execute(tuple);
    verify(writer, times(1)).write(eq(sensorType), any(ParserWriterConfiguration.class), eq(tuple), eq(finalMessage1));
    verify(outputCollector, times(1)).ack(tuple);
    when(parser.validate(eq(messages.get(0)))).thenReturn(true);
    when(parser.validate(eq(messages.get(1)))).thenReturn(true);
    when(filter.emitTuple(messages.get(0))).thenReturn(false);
    when(filter.emitTuple(messages.get(1))).thenReturn(true);
    parserBolt.withMessageFilter(filter);
    parserBolt.execute(tuple);
    verify(writer, times(1)).write(eq(sensorType), any(ParserWriterConfiguration.class), eq(tuple), eq(finalMessage2));
    verify(outputCollector, times(2)).ack(tuple);
    doThrow(new Exception()).when(writer).write(eq(sensorType), any(ParserWriterConfiguration.class), eq(tuple), eq(finalMessage2));
    parserBolt.execute(tuple);
    verify(outputCollector, times(1)).reportError(any(Throwable.class));
  }
@Test
public void testImplicitBatchOfOne() throws Exception {

  String sensorType = "yaf";

  ParserBolt parserBolt = new ParserBolt("zookeeperUrl", sensorType, parser, batchWriter) {
    @Override
    protected ParserConfigurations defaultConfigurations() {
      return new ParserConfigurations() {
        @Override
        public SensorParserConfig getSensorParserConfig(String sensorType) {
          return new SensorParserConfig() {
            @Override
            public Map<String, Object> getParserConfig() {
              return new HashMap<String, Object>() {{
              }};
            }
          };
        }
      };
    }
  };
  parserBolt.setCuratorFramework(client);
  parserBolt.setTreeCache(cache);
  parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
  verify(parser, times(1)).init();
  verify(batchWriter, times(1)).init(any(), any());
  when(parser.validate(any())).thenReturn(true);
  when(parser.parse(any())).thenReturn(ImmutableList.of(new JSONObject()));
  when(filter.emitTuple(any())).thenReturn(true);
  parserBolt.withMessageFilter(filter);
  parserBolt.execute(t1);
  verify(outputCollector, times(1)).ack(t1);
}

  /**
   {
    "filterClassName" : "QUERY"
   ,"parserConfig" : {
    "filter.query" : "exists(field1)"
    }
   }
   */
  @Multiline
  public static String sensorParserConfig;
  @Test
  public void testFilter() throws Exception {
    String sensorType = "yaf";
    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", sensorType, parser, batchWriter) {
      @Override
      protected SensorParserConfig getSensorParserConfig() {
        try {
          return SensorParserConfig.fromBytes(Bytes.toBytes(sensorParserConfig));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
    parserBolt.setCuratorFramework(client);
    parserBolt.setTreeCache(cache);
    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(parser, times(1)).init();
    verify(batchWriter, times(1)).init(any(), any());
    when(parser.validate(any())).thenReturn(true);
    when(parser.parse(any())).thenReturn(ImmutableList.of(new JSONObject()));
    parserBolt.withMessageFilter(filter);
    parserBolt.execute(t1);
    verify(outputCollector, times(1)).ack(t1);
  }
  @Test
  public void testBatchOfOne() throws Exception {

    String sensorType = "yaf";

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", sensorType, parser, batchWriter) {
      @Override
      protected ParserConfigurations defaultConfigurations() {
        return new ParserConfigurations() {
          @Override
          public SensorParserConfig getSensorParserConfig(String sensorType) {
            return new SensorParserConfig() {
              @Override
              public Map<String, Object> getParserConfig() {
                return new HashMap<String, Object>() {{
                  put(ParserWriterConfiguration.BATCH_CONF, "1");
                }};
              }
            };
          }
        };
      }
    };
    parserBolt.setCuratorFramework(client);
    parserBolt.setTreeCache(cache);
    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(parser, times(1)).init();
    verify(batchWriter, times(1)).init(any(), any());
    when(parser.validate(any())).thenReturn(true);
    when(parser.parse(any())).thenReturn(ImmutableList.of(new JSONObject()));
    when(filter.emitTuple(any())).thenReturn(true);
    parserBolt.withMessageFilter(filter);
    parserBolt.execute(t1);
    verify(outputCollector, times(1)).ack(t1);
  }
  @Test
  public void testBatchOfFive() throws Exception {

    String sensorType = "yaf";

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", sensorType, parser, batchWriter) {
      @Override
      protected ParserConfigurations defaultConfigurations() {
        return new ParserConfigurations() {
          @Override
          public SensorParserConfig getSensorParserConfig(String sensorType) {
            return new SensorParserConfig() {
              @Override
              public Map<String, Object> getParserConfig() {
                return new HashMap<String, Object>() {{
                  put(ParserWriterConfiguration.BATCH_CONF, 5);
                }};
              }
            };
          }
        };
      }
    };
    parserBolt.setCuratorFramework(client);
    parserBolt.setTreeCache(cache);
    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(parser, times(1)).init();
    verify(batchWriter, times(1)).init(any(), any());
    when(parser.validate(any())).thenReturn(true);
    when(parser.parse(any())).thenReturn(ImmutableList.of(new JSONObject()));
    when(filter.emitTuple(any())).thenReturn(true);
    parserBolt.withMessageFilter(filter);
    writeNonBatch(outputCollector, parserBolt, t1);
    writeNonBatch(outputCollector, parserBolt, t2);
    writeNonBatch(outputCollector, parserBolt, t3);
    writeNonBatch(outputCollector, parserBolt, t4);
    parserBolt.execute(t5);
    verify(outputCollector, times(1)).ack(t1);
    verify(outputCollector, times(1)).ack(t2);
    verify(outputCollector, times(1)).ack(t3);
    verify(outputCollector, times(1)).ack(t4);
    verify(outputCollector, times(1)).ack(t5);


  }
  @Test
  public void testBatchOfFiveWithError() throws Exception {

    String sensorType = "yaf";
    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", sensorType, parser, batchWriter) {
      @Override
      protected ParserConfigurations defaultConfigurations() {
        return new ParserConfigurations() {
          @Override
          public SensorParserConfig getSensorParserConfig(String sensorType) {
            return new SensorParserConfig() {
              @Override
              public Map<String, Object> getParserConfig() {
                return new HashMap<String, Object>() {{
                  put(ParserWriterConfiguration.BATCH_CONF, 5);
                }};
              }
            };
          }
        };
      }
    };
    parserBolt.setCuratorFramework(client);
    parserBolt.setTreeCache(cache);
    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(parser, times(1)).init();
    verify(batchWriter, times(1)).init(any(), any());

    doThrow(new Exception()).when(batchWriter).write(any(), any(), any(), any());
    when(parser.validate(any())).thenReturn(true);
    when(parser.parse(any())).thenReturn(ImmutableList.of(new JSONObject()));
    when(filter.emitTuple(any())).thenReturn(true);
    parserBolt.withMessageFilter(filter);
    writeNonBatch(outputCollector, parserBolt, t1);
    writeNonBatch(outputCollector, parserBolt, t2);
    writeNonBatch(outputCollector, parserBolt, t3);
    writeNonBatch(outputCollector, parserBolt, t4);
    parserBolt.execute(t5);
    verify(outputCollector, times(0)).ack(t1);
    verify(outputCollector, times(1)).fail(t1);
    verify(outputCollector, times(0)).ack(t2);
    verify(outputCollector, times(1)).fail(t2);
    verify(outputCollector, times(0)).ack(t3);
    verify(outputCollector, times(1)).fail(t3);
    verify(outputCollector, times(0)).ack(t4);
    verify(outputCollector, times(1)).fail(t4);
    verify(outputCollector, times(0)).ack(t5);
    verify(outputCollector, times(1)).fail(t5);


  }
  private static void writeNonBatch(OutputCollector collector, ParserBolt bolt, Tuple t) {
    bolt.execute(t);
    verify(collector, times(0)).ack(t);
  }

/*=======
    verify(writer, times(1)).init();
    byte[] sampleBinary = "some binary message".getBytes();
    JSONParser jsonParser = new JSONParser();
    final JSONObject sampleMessage1 = (JSONObject) jsonParser.parse("{ \"field1\":\"value1\" }");
    final JSONObject sampleMessage2 = (JSONObject) jsonParser.parse("{ \"field2\":\"value2\" }");
    List<JSONObject> messages = new ArrayList<JSONObject>() {{
      add(sampleMessage1);
      add(sampleMessage2);
    }};
    final JSONObject finalMessage1 = (JSONObject) jsonParser.parse("{ \"field1\":\"value1\", \"source.type\":\"" + sensorType + "\" }");
    when(tuple.getBinary(0)).thenReturn(sampleBinary);
    when(parser.parse(sampleBinary)).thenReturn(messages);
    when(parser.validate(any(JSONObject.class))).thenReturn(true);
    parserBolt.execute(tuple);
    verify(writer, times(1)).write(eq(sensorType), any(Configurations.class), eq(tuple), eq(finalMessage1));
    verify(outputCollector, times(1)).ack(tuple);
  }
>>>>>>> master*/
}
