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
import backtype.storm.tuple.Tuple;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.configuration.writer.SingleBatchConfigurationFacade;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.interfaces.MessageWriter;
import org.apache.metron.writer.BulkWriterComponent;
import org.apache.metron.writer.WriterToBulkWriter;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

public class WriterHandler implements Serializable {
  private BulkMessageWriter<JSONObject> messageWriter;
  private transient BulkWriterComponent<JSONObject> writerComponent;
  private transient Function<ParserConfigurations, WriterConfiguration> writerTransformer;
  private boolean isBulk = false;
  public WriterHandler(MessageWriter<JSONObject> writer) {
    isBulk = false;
    messageWriter = new WriterToBulkWriter<>(writer);

  }
  public WriterHandler(BulkMessageWriter<JSONObject> writer) {
    isBulk = true;
    messageWriter = writer;
  }


  public boolean handleAck() {
    return isBulk;
  }

  public void init(Map stormConf, OutputCollector collector, ParserConfigurations configurations) {
    if(isBulk) {
      writerTransformer = config -> new ParserWriterConfiguration(config);
    }
    else {
      writerTransformer = config -> new SingleBatchConfigurationFacade(new ParserWriterConfiguration(config));
    }
    try {
      messageWriter.init(stormConf, writerTransformer.apply(configurations));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to initialize message writer", e);
    }
    this.writerComponent = new BulkWriterComponent<JSONObject>(collector, isBulk, isBulk) {
      @Override
      protected Collection<Tuple> createTupleCollection() {
        return new HashSet<>();
      }
    };
  }

  public void write( String sensorType
                   , Tuple tuple
                   , JSONObject message
                   , ParserConfigurations configurations
                   ) throws Exception {
    writerComponent.write(sensorType, tuple, message, messageWriter, writerTransformer.apply(configurations));
  }

  public void errorAll(String sensorType, Throwable e) {
    writerComponent.errorAll(sensorType, e);
  }
}
