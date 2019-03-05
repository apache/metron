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
package org.apache.metron.common.message.metadata;

import java.util.Arrays;
import java.util.Map;

/**
 * A holder class for the message and metadata.
 */
public class RawMessage {
  byte[] message;
  Map<String, Object> metadata;

  public RawMessage(byte[] message, Map<String, Object> metadata) {
    this.message = message;
    this.metadata = metadata;
  }

  /**
   * Get the data to be parsed.
   * @return Raw bytes data of the message
   */
  public byte[] getMessage() {
    return message;
  }

  public void setMessage(byte[] message) {
    this.message = message;
  }

  /**
   * Get the metadata to use based on the RawMessageStrategy.
   * @return Map of the metadata
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  @Override
  public String toString() {
    return "RawMessage{" +
            "message=" + Arrays.toString(message) +
            ", metadata=" + metadata +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RawMessage that = (RawMessage) o;

    if (!Arrays.equals(getMessage(), that.getMessage())) return false;
    return getMetadata() != null ? getMetadata().equals(that.getMetadata()) : that.getMetadata() == null;

  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(getMessage());
    result = 31 * result + (getMetadata() != null ? getMetadata().hashCode() : 0);
    return result;
  }
}
