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
package org.apache.metron.common.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A set of utilities for converted a message to a SHA-256 hex-encoded hash string. Can be applied
 * to JSON messages (or a subset of fields of a JSON message), or raw bytes.
 */
public class HashUtils {

  /**
   * Produces a hash of a JSON message from a subset of the fields.
   *
   * @param message The JSON message to be hashed
   * @param hashFields The fields to hash
   * @return A string containing the resulting hash
   */
  public static String getMessageHash(JSONObject message, Collection<String> hashFields) {
    List<String> hashElements = hashFields.stream().map(errorField ->
            String.format("%s-%s", errorField, message.get(errorField))).collect(Collectors.toList());
    return DigestUtils.sha256Hex(String.join("|", hashElements).getBytes(UTF_8));
  }

  public static String getMessageHash(JSONObject message) {
    return DigestUtils.sha256Hex(message.toJSONString().getBytes(UTF_8));
  }

  public static String getMessageHash(byte[] bytes) {
    return DigestUtils.sha256Hex(bytes);
  }
}
