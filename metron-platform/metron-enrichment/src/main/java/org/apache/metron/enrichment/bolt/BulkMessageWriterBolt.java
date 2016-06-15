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

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredEnrichmentBolt;
import org.apache.metron.common.configuration.writer.EnrichmentWriterConfiguration;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterComponent;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BulkMessageWriterBolt extends ConfiguredEnrichmentBolt {

  private static final Logger LOG = LoggerFactory
          .getLogger(BulkMessageWriterBolt.class);
  private BulkMessageWriter<JSONObject> bulkMessageWriter;
  private BulkWriterComponent<JSONObject> writerComponent;
  private boolean flush;
  private Long flushIntervalInMs;
  public BulkMessageWriterBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  public BulkMessageWriterBolt withBulkMessageWriter(BulkMessageWriter<JSONObject > bulkMessageWriter) {
    this.bulkMessageWriter = bulkMessageWriter;
    return this;
  }

  public BulkMessageWriterBolt withFlush (boolean flush) {
    this.flush = flush;
    return this;
  }

  public BulkMessageWriterBolt withFlushIntervalInMs (Long flushIntervalInMs) {
    this.flushIntervalInMs = flushIntervalInMs;
    return this;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.writerComponent = new BulkWriterComponent<>(collector);
    this.writerComponent.setFlush(flush);
    this.writerComponent.setFlushIntervalInMs(flushIntervalInMs);
    super.prepare(stormConf, context, collector);
    try {
      bulkMessageWriter.init(stormConf, new EnrichmentWriterConfiguration(getConfigurations()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    JSONObject message =(JSONObject)tuple.getValueByField("message");
    String sensorType = MessageUtils.getSensorType(message);
    try
    {
      writerComponent.write(sensorType, tuple, message, bulkMessageWriter, new EnrichmentWriterConfiguration(getConfigurations()));
    }
    catch(Exception e) {
      throw new RuntimeException("This should have been caught in the writerComponent.  If you see this, file a JIRA", e);
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(Constants.ERROR_STREAM, new Fields("message"));
  }
}
