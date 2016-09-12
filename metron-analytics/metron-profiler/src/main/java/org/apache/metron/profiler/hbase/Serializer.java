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

/**
 * Serializes and deserializes values.
 *
 * The value produced by a Profile definition can be any numeric data type.  The data
 * type depends on how the profile is defined by the user.  The user should be able to
 * choose the data type that is most suitable for their use case.
 */
public interface Serializer {

  /**
   * Serialize a profile measurement's value.
   *
   * @param value The value to serialize.
   */
  byte[] toBytes(Number value);

  /**
   * Deserialize a profile measurement's value.
   *
   * @param value The value to deserialize.
   */
  <T extends Number> T fromBytes(byte[] value, Class<T> clazz);
}
