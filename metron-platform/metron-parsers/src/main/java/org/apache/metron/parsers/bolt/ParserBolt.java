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

import static org.apache.metron.common.Constants.METADATA_PREFIX;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredParserBolt;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.FieldValidator;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.MessageGetters;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserBolt extends ConfiguredParserBolt implements Serializable {

  private static final int KEY_INDEX = 1;
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private OutputCollector collector;
  private MessageParser<JSONObject> parser;
  //default filter is noop, so pass everything through.
  private MessageFilter<JSONObject> filter;
  private WriterHandler writer;
  private Context stellarContext;
  private transient MessageGetStrategy messageGetStrategy;
  public ParserBolt( String zookeeperUrl
                   , String sensorType
                   , MessageParser<JSONObject> parser
                   , WriterHandler writer
  )
  {
    super(zookeeperUrl, sensorType);
    this.writer = writer;
    this.parser = parser;
  }


  public ParserBolt withMessageFilter(MessageFilter<JSONObject> filter) {
    this.filter = filter;
    return this;
  }

  public MessageParser<JSONObject> getParser() {
    return parser;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);
    messageGetStrategy = MessageGetters.DEFAULT_BYTES_FROM_POSITION.get();
    this.collector = collector;
    initializeStellar();
    if(getSensorParserConfig() != null && filter == null) {
      getSensorParserConfig().getParserConfig().putIfAbsent("stellarContext", stellarContext);
      if (!StringUtils.isEmpty(getSensorParserConfig().getFilterClassName())) {
        filter = Filters.get(getSensorParserConfig().getFilterClassName()
                , getSensorParserConfig().getParserConfig()
        );
      }
    }

    parser.init();

    writer.init(stormConf, context, collector, getConfigurations());

    SensorParserConfig config = getSensorParserConfig();
    if(config != null) {
      config.init();
    }
    else {
      throw new IllegalStateException("Unable to retrieve a parser config for " + getSensorType());
    }
    parser.configure(config.getParserConfig());
  }

  protected void initializeStellar() {
    this.stellarContext = new Context.Builder()
                                .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                                .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
                                .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
                                .build();
    StellarFunctions.initialize(stellarContext);
  }

  private Map<String, Object> getMetadata(Tuple t, boolean readMetadata) {
    Map<String, Object> ret = new HashMap<>();
    if(!readMetadata) {
      return ret;
    }
    Fields tupleFields = t.getFields();
    for(int i = 2;i < tupleFields.size();++i) {
      String envMetadataFieldName = tupleFields.get(i);
      Object envMetadataFieldValue = t.getValue(i);
      if(!StringUtils.isEmpty(envMetadataFieldName) && envMetadataFieldValue != null) {
        ret.put(METADATA_PREFIX + envMetadataFieldName, envMetadataFieldValue);
      }
    }
    byte[] keyObj = t.getBinary(KEY_INDEX);
    String keyStr = null;
    try {
      keyStr = keyObj == null?null:new String(keyObj);
      if(!StringUtils.isEmpty(keyStr)) {
        Map<String, Object> metadata = JSONUtils.INSTANCE.load(keyStr,JSONUtils.MAP_SUPPLIER);
        for(Map.Entry<String, Object> kv : metadata.entrySet()) {
          ret.put(METADATA_PREFIX + kv.getKey(), kv.getValue());
        }

      }
    } catch (IOException e) {
        String reason = "Unable to parse metadata; expected JSON Map: " + (keyStr == null?"NON-STRING!":keyStr);
        LOG.error(reason, e);
        throw new IllegalStateException(reason, e);
      }
    return ret;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    byte[] originalMessage = (byte[]) messageGetStrategy.get(tuple);
    SensorParserConfig sensorParserConfig = getSensorParserConfig();
    try {
      //we want to ack the tuple in the situation where we have are not doing a bulk write
      //otherwise we want to defer to the writerComponent who will ack on bulk commit.
      boolean ackTuple = !writer.handleAck();
      int numWritten = 0;
      if(sensorParserConfig != null) {
        Map<String, Object> metadata = getMetadata(tuple, sensorParserConfig.getReadMetadata());
        List<FieldValidator> fieldValidations = getConfigurations().getFieldValidations();
        Optional<List<JSONObject>> messages = parser.parseOptional(originalMessage);
        for (JSONObject message : messages.orElse(Collections.emptyList())) {
          message.put(Constants.SENSOR_TYPE, getSensorType());
          if(sensorParserConfig.getMergeMetadata()) {
            message.putAll(metadata);
          }
          for (FieldTransformer handler : sensorParserConfig.getFieldTransformations()) {
            if (handler != null) {
              if(!sensorParserConfig.getMergeMetadata()) {
                //if we haven't merged metadata, then we need to pass them along as configuration params.
                handler.transformAndUpdate(message, stellarContext, sensorParserConfig.getParserConfig(), metadata);
              }
              else {
                handler.transformAndUpdate(message, stellarContext, sensorParserConfig.getParserConfig());
              }
            }
          }
          if(!message.containsKey(Constants.GUID)) {
            message.put(Constants.GUID, UUID.randomUUID().toString());
          }

          if (filter == null || filter.emitTuple(message, stellarContext)) {
            boolean isInvalid = !parser.validate(message);
            List<FieldValidator> failedValidators = null;
            if(!isInvalid) {
              failedValidators = getFailedValidators(message, fieldValidations);
              isInvalid = !failedValidators.isEmpty();
            }
            if( isInvalid) {
              MetronError error = new MetronError()
                      .withErrorType(Constants.ErrorType.PARSER_INVALID)
                      .withSensorType(getSensorType())
                      .addRawMessage(message);
              Set<String> errorFields = failedValidators == null?null:failedValidators.stream()
                      .flatMap(fieldValidator -> fieldValidator.getInput().stream())
                      .collect(Collectors.toSet());
              if (errorFields != null && !errorFields.isEmpty()) {
                error.withErrorFields(errorFields);
              }
              ErrorUtils.handleError(collector, error);
            }
            else {
              numWritten++;
              writer.write(getSensorType(), tuple, message, getConfigurations(), messageGetStrategy);
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
      handleError(originalMessage, tuple, ex, collector);
    }
  }

  protected void handleError(byte[] originalMessage, Tuple tuple, Throwable ex, OutputCollector collector) {
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(ex)
            .withSensorType(getSensorType())
            .addRawMessage(originalMessage);
    ErrorUtils.handleError(collector, error);
    collector.ack(tuple);
  }

  private List<FieldValidator> getFailedValidators(JSONObject input, List<FieldValidator> validators) {
    List<FieldValidator> failedValidators = new ArrayList<>();
    for(FieldValidator validator : validators) {
      if(!validator.isValid(input, getConfigurations().getGlobalConfig(), stellarContext)) {
        failedValidators.add(validator);
      }
    }
    return failedValidators;
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(Constants.ERROR_STREAM, new Fields("message"));
  }
}
