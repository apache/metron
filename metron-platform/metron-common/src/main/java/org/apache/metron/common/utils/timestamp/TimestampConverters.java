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

package org.apache.metron.common.utils.timestamp;


import com.google.common.base.Joiner;

public enum TimestampConverters implements TimestampConverter{
  MILLISECONDS(tsMilli -> tsMilli*1000000L)
  ,MICROSECONDS(tsMicro -> tsMicro*1000L)
  ,NANOSECONDS(tsNano -> tsNano);
  TimestampConverter converter;
  TimestampConverters(TimestampConverter converter) {
    this.converter = converter;
  }

  /**
   * Gets a {@link TimestampConverter} by name.
   *
   * @param converter The name of the converter to get
   * @return The timestamp converter
   */
  public static TimestampConverter getConverter(String converter) {
    if(converter != null) {
      return TimestampConverters.valueOf(converter.toUpperCase()).converter;
    }
    else {
      throw new IllegalStateException(converter + " is not a valid timestamp converter: "
              + Joiner.on(",").join(TimestampConverters.values()));
    }
  }

  @Override
  public long toNanoseconds(long in) {
    return converter.toNanoseconds(in);
  }
}
