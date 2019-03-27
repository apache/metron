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

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.MessageGetters;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.writer.AckTuplesPolicy;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.Map;

public class WriterBolt extends BaseRichBolt {
  private WriterHandler handler;
  private ParserConfigurations configuration;
  private String sensorType;
  private Constants.ErrorType errorType = Constants.ErrorType.DEFAULT_ERROR;
  //In test scenarios, maxBatchTimeout may not be correctly initialized, so do it here.
  //This is a conservative maxBatchTimeout for a vanilla bolt with batchTimeoutDivisor=2
  public static final int UNINITIALIZED_MAX_BATCH_TIMEOUT = 6;
  private transient MessageGetStrategy messageGetStrategy;
  private transient OutputCollector collector;
  private transient AckTuplesPolicy ackTuplesPolicy;
  public WriterBolt(WriterHandler handler, ParserConfigurations configuration, String sensorType) {
    this.handler = handler;
    this.configuration = configuration;
    this.sensorType = sensorType;
  }

  public WriterBolt withErrorType(Constants.ErrorType errorType) {
    this.errorType = errorType;
    return this;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
    messageGetStrategy = MessageGetters.DEFAULT_JSON_FROM_FIELD.get();
    ackTuplesPolicy = new AckTuplesPolicy(collector, messageGetStrategy);
    handler.init(stormConf, context, collector, configuration, ackTuplesPolicy, UNINITIALIZED_MAX_BATCH_TIMEOUT);
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
      message = (JSONObject) messageGetStrategy.get(tuple);
      String messageId = MessageUtils.getGuid(message);
      ackTuplesPolicy.addTupleMessageIds(tuple, Collections.singleton(messageId));
      handler.write(sensorType, new BulkMessage<>(messageId, message), configuration);
    } catch (Throwable e) {
      MetronError error = new MetronError()
              .withErrorType(errorType)
              .withThrowable(e)
              .withSensorType(Collections.singleton(sensorType))
              .addRawMessage(message);
      ErrorUtils.handleError(collector, error);
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
