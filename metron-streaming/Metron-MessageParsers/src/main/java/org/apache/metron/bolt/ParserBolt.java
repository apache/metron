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
package org.apache.metron.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import org.apache.metron.Constants;
import org.apache.metron.filters.GenericMessageFilter;
import org.apache.metron.helpers.topology.ErrorUtils;
import org.apache.metron.parser.interfaces.MessageFilter;
import org.apache.metron.parser.interfaces.MessageParser;
import org.apache.metron.writer.interfaces.MessageWriter;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class ParserBolt extends ConfiguredBolt {

  private OutputCollector collector;
  private MessageParser<JSONObject> parser;
  private MessageFilter<JSONObject> filter = new GenericMessageFilter();
  private MessageWriter<JSONObject> writer;
  private String sensorType;

  public ParserBolt(String zookeeperUrl, String sensorType, MessageParser<JSONObject> parser, MessageWriter<JSONObject> writer) {
    super(zookeeperUrl);
    this.parser = parser;
    this.sensorType = sensorType;
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
    parser.init();
    writer.init();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    byte[] originalMessage = tuple.getBinary(0);
    try {
      List<JSONObject> messages = parser.parse(originalMessage);
      for(JSONObject message: messages) {
        if (parser.validate(message)) {
          if (filter != null && filter.emitTuple(message)) {
            message.put(Constants.SENSOR_TYPE, sensorType);
            writer.write(sensorType, configurations, tuple, message);
          }
        }
      }
      collector.ack(tuple);
    } catch (Throwable ex) {
      ErrorUtils.handleError(collector, ex, Constants.ERROR_STREAM);
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }
}
