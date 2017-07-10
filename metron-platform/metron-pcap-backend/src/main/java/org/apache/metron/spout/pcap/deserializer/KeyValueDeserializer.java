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

package org.apache.metron.spout.pcap.deserializer;

import org.apache.metron.common.utils.timestamp.TimestampConverter;
import org.apache.metron.common.utils.timestamp.TimestampConverters;

import java.io.Serializable;

public abstract class KeyValueDeserializer implements Serializable {
  protected TimestampConverter converter;

  public static class Result {
    public byte[] value;
    public Long key;
    public boolean foundTimestamp;
    public Result(Long key, byte[] value, boolean foundTimestamp) {
      this.key = key;
      this.value = value;
      this.foundTimestamp = foundTimestamp;
    }
  }

  public KeyValueDeserializer() {
    this(TimestampConverters.MICROSECONDS);
  }

  public KeyValueDeserializer(TimestampConverter converter) {
    this.converter = converter;
  }

  public abstract Result deserializeKeyValue(byte[] key, byte[] value);

}
