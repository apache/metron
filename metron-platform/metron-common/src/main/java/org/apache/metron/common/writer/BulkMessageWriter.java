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

import org.apache.storm.task.TopologyContext;
import org.apache.metron.common.configuration.writer.WriterConfiguration;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface BulkMessageWriter<MESSAGE_T> extends AutoCloseable, Serializable {

  void init(Map stormConf, TopologyContext topologyContext, WriterConfiguration config) throws Exception;

  /**
  * Writes the messages to a particular output (e.g. Elasticsearch).  A response is returned with successful and failed message ids.
  * @param sensorType The type of sensor generating the messages
  * @param configurations Configurations that should be passed to the writer (e.g. index and
  * @param messages  A list of messages to be written.  Message ids are used in the response to report successes/failures.
  * @return A response containing successes and failures within the batch.
  * @throws Exception If an unrecoverable error is made, an Exception is thrown which should be treated as a full-batch failure (e.g. target system is down).
  */
  BulkWriterResponse write(String sensorType
            , WriterConfiguration configurations
            , List<BulkMessage<MESSAGE_T>> messages
            ) throws Exception;

  String getName();
}
