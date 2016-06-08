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

package org.apache.metron.common.writer;

import backtype.storm.tuple.Tuple;
import com.google.common.collect.Iterables;
import org.apache.metron.common.configuration.writer.SingleBatchConfigurationFacade;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.interfaces.MessageWriter;

import java.util.List;
import java.util.Map;

public class WriterToBulkWriter<MESSAGE_T> implements BulkMessageWriter<MESSAGE_T> {
  MessageWriter<MESSAGE_T> messageWriter;

  public WriterToBulkWriter(MessageWriter<MESSAGE_T> messageWriter) {
    this.messageWriter = messageWriter;
  }
  @Override
  public void init(Map stormConf, WriterConfiguration config) throws Exception {
    messageWriter.init();
  }

  @Override
  public void write(String sensorType, WriterConfiguration configurations, Iterable<Tuple> tuples, List<MESSAGE_T> messages) throws Exception {
    if(messages.size() > 1) {
      throw new IllegalStateException("WriterToBulkWriter expects a batch of exactly 1");
    }
    messageWriter.write(sensorType, configurations, Iterables.getFirst(tuples, null), Iterables.getFirst(messages, null));
  }

  @Override
  public void close() throws Exception {
    messageWriter.close();
  }
}
