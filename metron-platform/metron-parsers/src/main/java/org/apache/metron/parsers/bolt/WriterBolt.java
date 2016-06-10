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
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.utils.ErrorUtils;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.Optional;

public class WriterBolt extends BaseRichBolt {
  private WriterHandler handler;
  private ParserConfigurations configuration;
  private String sensorType;
  private transient OutputCollector collector;
  public WriterBolt(WriterHandler handler, ParserConfigurations configuration, String sensorType) {
    this.handler = handler;
    this.configuration = configuration;
    this.sensorType = sensorType;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
    handler.init(stormConf, collector, configuration);
  }

  private JSONObject getMessage(Tuple tuple) {
    Object ret = tuple.getValueByField("message");
    if(ret != null) {
      ret = tuple.getValue(0);
    }
    if(ret != null) {
      return (JSONObject)((JSONObject)ret).clone();
    }
    else {
      return null;
    }
  }

  @Override
  public void execute(Tuple tuple) {
    JSONObject message = null;
    try {
      message = (JSONObject)((JSONObject) tuple.getValueByField("message")).clone();
      handler.write(sensorType, tuple, message, configuration);
      if(!handler.handleAck()) {
        collector.ack(tuple);
      }
    } catch (Throwable e) {
      ErrorUtils.handleError( collector
                            , e
                            , Constants.ERROR_STREAM
                            , Optional.of(sensorType)
                            , Optional.ofNullable(message)
                            );
      collector.ack(tuple);
    }
  }

  /**
   * Declare the output schema for all the streams of this topology.
   *
   * @param declarer this is used to declare output stream ids, output fields, and whether or not each output stream is a direct stream
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }
}
