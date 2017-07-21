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
package org.apache.metron.writer.bolt;

import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredIndexingBolt;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.MessageGetters;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.MessageWriter;
import org.apache.metron.writer.BulkWriterComponent;
import org.apache.metron.writer.WriterToBulkWriter;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

public class BulkMessageWriterBolt extends ConfiguredIndexingBolt {

  private static final Logger LOG = LoggerFactory
          .getLogger(BulkMessageWriterBolt.class);
  private BulkMessageWriter<JSONObject> bulkMessageWriter;
  private BulkWriterComponent<JSONObject> writerComponent;
  private String messageGetStrategyType = MessageGetters.DEFAULT_JSON_FROM_FIELD.name();
  private String messageGetField;
  private transient MessageGetStrategy messageGetStrategy;
  private transient OutputCollector collector;
  private transient Function<WriterConfiguration, WriterConfiguration> configurationTransformation;
  public BulkMessageWriterBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  public BulkMessageWriterBolt withBulkMessageWriter(BulkMessageWriter<JSONObject > bulkMessageWriter) {
    this.bulkMessageWriter = bulkMessageWriter;
    return this;
  }

  public BulkMessageWriterBolt withMessageWriter(MessageWriter<JSONObject> messageWriter) {
    this.bulkMessageWriter = new WriterToBulkWriter<>(messageWriter);
    return this;
  }

  public BulkMessageWriterBolt withMessageGetter(String messageGetStrategyType) {
    this.messageGetStrategyType = messageGetStrategyType;
    return this;
  }

  public BulkMessageWriterBolt withMessageGetterField(String messageGetField) {
    this.messageGetField = messageGetField;
    return this;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.writerComponent = new BulkWriterComponent<>(collector);
    this.collector = collector;
    super.prepare(stormConf, context, collector);
    if (messageGetField != null) {
      messageGetStrategy = MessageGetters.valueOf(messageGetStrategyType).get(messageGetField);
    } else {
      messageGetStrategy = MessageGetters.valueOf(messageGetStrategyType).get();
    }
    if(bulkMessageWriter instanceof WriterToBulkWriter) {
      configurationTransformation = WriterToBulkWriter.TRANSFORMATION;
    }
    else {
      configurationTransformation = x -> x;
    }
    try {
      bulkMessageWriter.init(stormConf,
                             context,
                             configurationTransformation.apply(new IndexingWriterConfiguration(bulkMessageWriter.getName(),
                             getConfigurations()))
                            );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    JSONObject message = (JSONObject) messageGetStrategy.get(tuple);
    String sensorType = MessageUtils.getSensorType(message);
    try
    {
      WriterConfiguration writerConfiguration = configurationTransformation.apply(new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations()));
      if(writerConfiguration.isDefault(sensorType)) {
        //want to warn, but not fail the tuple
        collector.reportError(new Exception("WARNING: Default and (likely) unoptimized writer config used for " + bulkMessageWriter.getName() + " writer and sensor " + sensorType));
      }
      writerComponent.write(sensorType
                           , tuple
                           , message
                           , bulkMessageWriter
                           , writerConfiguration
                           , messageGetStrategy
                           );
      LOG.trace("Writing enrichment message: {}", message);
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
