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

import java.lang.invoke.MethodHandles;
import org.apache.metron.common.utils.timestamp.TimestampConverter;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.spout.pcap.Endianness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extract the timestamp from the key and raw data from the packet.
 */
public class FromKeyDeserializer extends KeyValueDeserializer {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static Endianness endianness = Endianness.getNativeEndianness();


  public FromKeyDeserializer(TimestampConverter converter) {
    super(converter);
  }

  @Override
  public Result deserializeKeyValue(byte[] key, byte[] value) {
    if (key == null) {
      throw new IllegalArgumentException("Expected a key but none provided");
    }
    long ts = converter.toNanoseconds(fromBytes(key));
    return new Result(ts, PcapHelper.addHeaders(ts, value, endianness), true);
  }

  /**
   * Convert the byte array representation for a long into a proper long.
   * @param data
   * @return a long
   */
  private static long fromBytes(byte[] data) {
    long value = 0L;
    int len = data.length;

    for(int i = 0; i < len; ++i) {
      byte b = data[i];
      //make room in the long
      value <<= 8;
      //drop the byte in
      value |= (long)(b & 255);
    }

    return value;
  }
}
