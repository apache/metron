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
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredParserBolt;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.filters.GenericMessageFilter;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.common.interfaces.MessageWriter;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class ParserBolt extends ConfiguredParserBolt {

  private OutputCollector collector;
  private MessageParser<JSONObject> parser;
  private MessageFilter<JSONObject> filter;
  private MessageWriter<JSONObject> writer;
  private long ingest_timestamp = 0;

  public ParserBolt(String zookeeperUrl, String sensorType, MessageParser<JSONObject> parser, MessageWriter<JSONObject> writer) {
    super(zookeeperUrl, sensorType);
    this.parser = parser;
    this.writer = writer;
  }

  public ParserBolt withMessageFilter(MessageFilter<JSONObject> filter) {
    this.filter = filter;
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);
    this.collector = collector;
    if(getSensorParserConfig() == null) {
      filter = new GenericMessageFilter();
    }
    else if(filter == null) {
      filter = Filters.get(getSensorParserConfig().getFilterClassName()
              , getSensorParserConfig().getParserConfig()
      );
    }
    parser.init();
    writer.init();
    SensorParserConfig config = getSensorParserConfig();
    if(config != null) {
      config.init();
    }
    else {
      throw new IllegalStateException("Unable to retrieve a parser config for " + getSensorType());
    }
  }


  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    byte[] originalMessage = tuple.getBinary(0);
    SensorParserConfig sensorParserConfig = getSensorParserConfig();
    try {
      if(sensorParserConfig != null) {
        List<JSONObject> messages = parser.parse(originalMessage);
        for (JSONObject message : messages) {
          if (parser.validate(message)) {
            if (filter != null && filter.emitTuple(message)) {
              ingest_timestamp = System.currentTimeMillis();
              message.put(Constants.SENSOR_TYPE, getSensorType());
              message.put("ingest_timestamp", ingest_timestamp);
              for (FieldTransformer handler : sensorParserConfig.getFieldTransformations()) {
                if (handler != null) {
                  handler.transformAndUpdate(message, sensorParserConfig.getParserConfig());
                }
              }
              writer.write(getSensorType(), configurations, tuple, message);
            }
          }
        }
      }
      collector.ack(tuple);
    } catch (Throwable ex) {
      ErrorUtils.handleError(collector, ex, Constants.ERROR_STREAM);
      collector.fail(tuple);
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }
}
