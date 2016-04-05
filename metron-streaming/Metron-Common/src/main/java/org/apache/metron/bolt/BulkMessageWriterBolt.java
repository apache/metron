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
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import org.apache.metron.Constants;
import org.apache.metron.domain.SensorEnrichmentConfig;
import org.apache.metron.helpers.topology.ErrorUtils;
import org.apache.metron.topology.TopologyUtils;
import org.apache.metron.writer.interfaces.BulkMessageWriter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BulkMessageWriterBolt extends ConfiguredBolt {

  private static final Logger LOG = LoggerFactory
          .getLogger(BulkMessageWriterBolt.class);
  private OutputCollector collector;
  private BulkMessageWriter<JSONObject> bulkMessageWriter;
  private Map<String, List<Tuple>> sensorTupleMap = new HashMap<>();
  private Map<String, List<JSONObject>> sensorMessageMap = new HashMap<>();

  public BulkMessageWriterBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  public BulkMessageWriterBolt withBulkMessageWriter(BulkMessageWriter<JSONObject> bulkMessageWriter) {
    this.bulkMessageWriter = bulkMessageWriter;
    return this;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
    super.prepare(stormConf, context, collector);
    try {
      bulkMessageWriter.init(stormConf, configurations);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    JSONObject message = (JSONObject)((JSONObject) tuple.getValueByField("message")).clone();
    message.put("index." + bulkMessageWriter.getClass().getSimpleName().toLowerCase() + ".ts", "" + System.currentTimeMillis());
    String sensorType = TopologyUtils.getSensorType(message);
    SensorEnrichmentConfig sensorEnrichmentConfig = configurations.getSensorEnrichmentConfig(sensorType);
    int batchSize = sensorEnrichmentConfig != null ? sensorEnrichmentConfig.getBatchSize() : 1;
    List<Tuple> tupleList = sensorTupleMap.get(sensorType);
    if (tupleList == null) tupleList = new ArrayList<>();
    tupleList.add(tuple);
    List<JSONObject> messageList = sensorMessageMap.get(sensorType);
    if (messageList == null) messageList = new ArrayList<>();
    messageList.add(message);
    if (messageList.size() < batchSize) {
      sensorTupleMap.put(sensorType, tupleList);
      sensorMessageMap.put(sensorType, messageList);
    } else {
      try {
        String esType = sensorType + "_doc";
        bulkMessageWriter.write(esType, configurations, tupleList, messageList);
        for(Tuple t: tupleList) {
          collector.ack(t);
        }
      } catch (Exception e) {
        for(Tuple t: tupleList) {
          collector.fail(t);
        }
        ErrorUtils.handleError(collector, e, Constants.ERROR_STREAM);
      }
      sensorTupleMap.remove(sensorType);
      sensorMessageMap.remove(sensorType);
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("error", new Fields("message"));
  }
}
