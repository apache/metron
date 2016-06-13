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
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredParserBolt;
import org.apache.metron.common.configuration.FieldValidator;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.configuration.writer.SingleBatchConfigurationFacade;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterComponent;
import org.apache.metron.common.writer.WriterToBulkWriter;
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

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

public class ParserBolt extends ConfiguredParserBolt implements Serializable {

  private OutputCollector collector;
  private MessageParser<JSONObject> parser;
  private MessageFilter<JSONObject> filter = new GenericMessageFilter();
  private transient Function<ParserConfigurations, WriterConfiguration> writerTransformer;
  private BulkMessageWriter<JSONObject> messageWriter;
  private BulkWriterComponent<JSONObject> writerComponent;
  private boolean isBulk = false;
  public ParserBolt( String zookeeperUrl
                   , String sensorType
                   , MessageParser<JSONObject> parser
                   , MessageWriter<JSONObject> writer
  )
  {
    super(zookeeperUrl, sensorType);
    isBulk = false;
    this.parser = parser;
    messageWriter = new WriterToBulkWriter<>(writer);
  }

  public ParserBolt( String zookeeperUrl
                   , String sensorType
                   , MessageParser<JSONObject> parser
                   , BulkMessageWriter<JSONObject> writer
  )
  {
    super(zookeeperUrl, sensorType);
    isBulk = true;
    this.parser = parser;
    messageWriter = writer;


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

    if(isBulk) {
      writerTransformer = config -> new ParserWriterConfiguration(config);
    }
    else {
      writerTransformer = config -> new SingleBatchConfigurationFacade(new ParserWriterConfiguration(config));
    }
    try {
      messageWriter.init(stormConf, writerTransformer.apply(getConfigurations()));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to initialize message writer", e);
    }
    this.writerComponent = new BulkWriterComponent<JSONObject>(collector, isBulk, isBulk) {
      @Override
      protected Collection<Tuple> createTupleCollection() {
        return new HashSet<>();
      }
    };
    SensorParserConfig config = getSensorParserConfig();
    if(config != null) {
      config.init();
    }
    else {
      throw new IllegalStateException("Unable to retrieve a parser config for " + getSensorType());
    }
    parser.configure(config.getParserConfig());
  }


  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    byte[] originalMessage = tuple.getBinary(0);
    SensorParserConfig sensorParserConfig = getSensorParserConfig();
    try {
      //we want to ack the tuple in the situation where we have are not doing a bulk write
      //otherwise we want to defer to the writerComponent who will ack on bulk commit.
      boolean ackTuple = !isBulk;
      int numWritten = 0;
      if(sensorParserConfig != null) {
        List<FieldValidator> fieldValidations = getConfigurations().getFieldValidations();
        Optional<List<JSONObject>> messages = parser.parseOptional(originalMessage);
        for (JSONObject message : messages.orElse(Collections.emptyList())) {
          if (parser.validate(message) && filter != null && filter.emitTuple(message)) {
            message.put(Constants.SENSOR_TYPE, getSensorType());
            for (FieldTransformer handler : sensorParserConfig.getFieldTransformations()) {
              if (handler != null) {
                handler.transformAndUpdate(message, sensorParserConfig.getParserConfig());
              }
            }
            if(!isGloballyValid(message, fieldValidations)) {
              message.put(Constants.SENSOR_TYPE, getSensorType()+ ".invalid");
              collector.emit(Constants.INVALID_STREAM, new Values(message));
            }
            else {
              numWritten++;
              writerComponent.write(getSensorType(), tuple, message, messageWriter, writerTransformer.apply(getConfigurations()));
            }
          }
        }
      }
      //if we are supposed to ack the tuple OR if we've never passed this tuple to the bulk writer
      //(meaning that none of the messages are valid either globally or locally)
      //then we want to handle the ack ourselves.
      if(ackTuple || numWritten == 0) {
        collector.ack(tuple);
      }
    } catch (Throwable ex) {
      ErrorUtils.handleError(collector, ex, Constants.ERROR_STREAM);
      collector.ack(tuple);
    }
  }

  private boolean isGloballyValid(JSONObject input, List<FieldValidator> validators) {
    for(FieldValidator validator : validators) {
      if(!validator.isValid(input, getConfigurations().getGlobalConfig())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(Constants.INVALID_STREAM, new Fields("message"));
    declarer.declareStream(Constants.ERROR_STREAM, new Fields("message"));
  }
}
