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

import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.common.utils.timestamp.TimestampConverter;

import java.util.function.Function;

/**
 * Deserializers take the raw bytes from kafka key and value and construct the timestamp and raw bytes for PCAP.
 */
public enum Deserializers {
  /**
   * Extract the timestamp from the key and the raw packet (global-headerless) from the value
   */
   FROM_KEY( converter -> new FromKeyDeserializer(converter))
  /**
   * Ignore the key and pull the timestamp directly from the packet itself.  Also, assume that the packet isn't global-headerless.
   */
  ,FROM_PACKET(converter -> new FromPacketDeserializer());
  ;
  Function<TimestampConverter, KeyValueDeserializer> creator;
  Deserializers(Function<TimestampConverter, KeyValueDeserializer> creator)
  {
    this.creator = creator;
  }

  public static KeyValueDeserializer create(String scheme, TimestampConverter converter) {
    try {
      Deserializers ts = Deserializers.valueOf(scheme.toUpperCase());
      return ts.creator.apply(converter);
    }
    catch(IllegalArgumentException iae) {
      return Deserializers.FROM_KEY.creator.apply(converter);
    }
  }

  public static KeyValueDeserializer create(String scheme, String converter) {
    return create(scheme, TimestampConverters.getConverter(converter));
  }

}
