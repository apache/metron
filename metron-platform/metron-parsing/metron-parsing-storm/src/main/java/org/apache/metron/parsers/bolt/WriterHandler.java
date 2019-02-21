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

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.function.Function;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.writer.ConfigurationStrategy;
import org.apache.metron.common.configuration.writer.ConfigurationsStrategies;
import org.apache.metron.common.configuration.writer.SingleBatchConfigurationFacade;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.MessageWriter;
import org.apache.metron.writer.AckTuplesPolicy;
import org.apache.metron.writer.BulkWriterComponent;
import org.apache.metron.writer.WriterToBulkWriter;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriterHandler implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private BulkMessageWriter<JSONObject> messageWriter;
  private transient BulkWriterComponent<JSONObject> writerComponent;
  private transient Function<ParserConfigurations, WriterConfiguration> writerTransformer;
  private boolean isBulk;
  private ConfigurationStrategy configStrategy = ConfigurationsStrategies.PARSERS;

  public WriterHandler(MessageWriter<JSONObject> writer) {
    isBulk = false;
    messageWriter = new WriterToBulkWriter<>(writer);

  }
  public WriterHandler(BulkMessageWriter<JSONObject> writer) {
    isBulk = true;
    messageWriter = writer;
  }

  public boolean isWriterToBulkWriter() {
    return messageWriter instanceof  WriterToBulkWriter;
  }

  public BulkMessageWriter getBulkMessageWriter() {
    return messageWriter;
  }

  public void init(Map stormConf, TopologyContext topologyContext, OutputCollector collector, ParserConfigurations configurations,
                   AckTuplesPolicy ackTuplesPolicy, int maxBatchTimeout) {
    if(isBulk) {
      writerTransformer = config -> configStrategy.createWriterConfig(messageWriter, config);
    }
    else {
      writerTransformer = config -> new SingleBatchConfigurationFacade(configStrategy.createWriterConfig(messageWriter, config));
    }
    try {
      messageWriter.init(stormConf, topologyContext, writerTransformer.apply(configurations));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to initialize message writer", e);
    }
    this.writerComponent = new BulkWriterComponent<>(maxBatchTimeout);
    this.writerComponent.addFlushPolicy(ackTuplesPolicy);
  }

  public void write(String sensorType
          , BulkMessage<JSONObject> bulkWriterMessage
          , ParserConfigurations configurations
  ) throws Exception {
    writerComponent.write(sensorType, bulkWriterMessage, messageWriter, writerTransformer.apply(configurations));
  }

  public void flush(ParserConfigurations configurations, MessageGetStrategy messageGetStrategy)
      throws Exception {
    if (!(messageWriter instanceof WriterToBulkWriter)) {
      //WriterToBulkWriter doesn't allow batching, so no need to flush on Tick.
      LOG.debug("Flushing message queues older than their batchTimeouts");
      writerComponent.flushAll(messageWriter, writerTransformer.apply(configurations));
    }
  }

}
