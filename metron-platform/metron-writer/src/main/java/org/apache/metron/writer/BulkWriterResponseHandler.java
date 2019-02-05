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

import org.apache.metron.common.writer.BulkWriterResponse;

/**
 * This interface is used by the {@link org.apache.metron.writer.BulkWriterComponent} to report that a queue for a
 * sensor type has been flushed.  Different frameworks may have different requirements for committing processed messages
 * so this abstraction provides a way to pass in the appropriate commit logic for the framework in use.
 */
public interface BulkWriterResponseHandler {

  /**
   * Called immediately after a batch has been flushed.
   * @param sensorType The type of sensor generating the messages
   * @param response A response containing successes and failures within the batch
   */
  void handleFlush(String sensorType, BulkWriterResponse response);
}
