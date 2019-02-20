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

package org.apache.metron.writer;

import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.MessageId;
import org.apache.storm.task.TopologyContext;
import com.google.common.collect.Iterables;
import org.apache.metron.common.configuration.writer.SingleBatchConfigurationFacade;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.MessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WriterToBulkWriter<MESSAGE_T> implements BulkMessageWriter<MESSAGE_T>, Serializable {
  MessageWriter<MESSAGE_T> messageWriter;

  public static transient Function<WriterConfiguration, WriterConfiguration> TRANSFORMATION = config -> new SingleBatchConfigurationFacade(config);

  public WriterToBulkWriter(MessageWriter<MESSAGE_T> messageWriter) {
    this.messageWriter = messageWriter;
  }
  @Override
  public void init(Map stormConf, TopologyContext topologyContext, WriterConfiguration config) throws Exception {
    messageWriter.init();
  }

  @Override
  public BulkWriterResponse write(String sensorType, WriterConfiguration configurations, List<BulkMessage<MESSAGE_T>> messages) throws Exception {
    Set<MessageId> ids = messages.stream().map(BulkMessage::getId).collect(Collectors.toSet());
    BulkWriterResponse response = new BulkWriterResponse();
    if(messages.size() > 1) {
        response.addAllErrors(new IllegalStateException("WriterToBulkWriter expects a batch of exactly 1"), ids);
        return response;
    }

    try {
      messageWriter.write(sensorType, configurations, Iterables.getFirst(messages, null));
    } catch(Exception e) {
      response.addAllErrors(e, ids);
      return response;
    }

    response.addAllSuccesses(ids);
    return response;
  }

  @Override
  public String getName() {
    return messageWriter.getName();
  }

  @Override
  public void close() throws Exception {
    messageWriter.close();
  }
}
