/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.profiler.hbase;

import org.apache.hadoop.hbase.util.Bytes;

/**
 * Provides basic functionality to serialize and deserialize the allowed
 * value types for a ProfileMeasurement.
 */
public class Serializer {

  private Serializer() {
    // do not instantiate
  }

  /**
   * Serialize a profile measurement's value.
   *
   * The value produced by a Profile definition can be any numeric data type.  The data
   * type depends on how the profile is defined by the user.  The user should be able to
   * choose the data type that is most suitable for their use case.
   *
   * @param value The value to serialize.
   */
  public static byte[] toBytes(Object value) {
    byte[] result;

    if(value instanceof Integer) {
      result = Bytes.toBytes((Integer) value);
    } else if(value instanceof Double) {
      result = Bytes.toBytes((Double) value);
    } else if(value instanceof Short) {
      result = Bytes.toBytes((Short) value);
    } else if(value instanceof Long) {
      result = Bytes.toBytes((Long) value);
    } else if(value instanceof Float) {
      result = Bytes.toBytes((Float) value);
    } else {
      throw new RuntimeException("Expected 'Number': actual=" + value);
    }

    return result;
  }

  /**
   * Deserialize a profile measurement's value.
   *
   * The value produced by a Profile definition can be any numeric data type.  The data
   * type depends on how the profile is defined by the user.  The user should be able to
   * choose the data type that is most suitable for their use case.
   *
   * @param value The value to deserialize.
   */
  public static <T> T fromBytes(byte[] value, Class<T> clazz) {
    T result;

    if(clazz == Integer.class) {
      result = clazz.cast(Bytes.toInt(value));
    } else if(clazz == Double.class) {
      result = clazz.cast(Bytes.toDouble(value));
    } else if(clazz == Short.class) {
      result = clazz.cast(Bytes.toShort(value));
    } else if(clazz == Long.class) {
      result = clazz.cast(Bytes.toLong(value));
    } else if(clazz == Float.class) {
      result = clazz.cast(Bytes.toFloat(value));
    } else {
      throw new RuntimeException("Expected 'Number': actual=" + clazz);
    }

    return result;
  }
}
