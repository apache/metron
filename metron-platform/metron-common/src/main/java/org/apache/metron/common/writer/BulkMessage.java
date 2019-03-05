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

import java.util.Objects;

public class BulkMessage<MESSAGE_T> {

  private MessageId id;
  private MESSAGE_T message;

  public BulkMessage(MessageId id, MESSAGE_T message) {
    this.id = id;
    this.message = message;
  }

  public BulkMessage(String id, MESSAGE_T message) {
    this(new MessageId(id), message);
  }

  public MessageId getId() {
    return id;
  }

  public MESSAGE_T getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BulkMessage<?> that = (BulkMessage<?>) o;
    return Objects.equals(id, that.id) &&
            Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, message);
  }
}
