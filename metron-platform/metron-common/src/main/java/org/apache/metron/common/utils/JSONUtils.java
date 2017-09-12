/*
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonPatch;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public enum JSONUtils {
  INSTANCE;

  private static ThreadLocal<JSONParser> _parser = ThreadLocal.withInitial(() ->
      new JSONParser());

  private static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  public <T> T convert(Object original, Class<T> targetClass) {
    return _mapper.get().convertValue(original, targetClass);
  }

  public ObjectMapper getMapper() {
    return _mapper.get();
  }


  public <T> T load(InputStream is, TypeReference<T> ref) throws IOException {
    return _mapper.get().readValue(is, ref);
  }

  public <T> T load(String is, TypeReference<T> ref) throws IOException {
    return _mapper.get().readValue(is, ref);
  }

  public <T> T load(File f, TypeReference<T> ref) throws IOException {
    try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
      return _mapper.get().readValue(is, ref);
    }
  }

  public <T> T load(InputStream is, Class<T> clazz) throws IOException {
    return _mapper.get().readValue(is, clazz);
  }

  public <T> T load(File f, Class<T> clazz) throws IOException {
    try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
      return _mapper.get().readValue(is, clazz);
    }
  }

  public <T> T load(String is, Class<T> clazz) throws IOException {
    return _mapper.get().readValue(is, clazz);
  }

  public String toJSON(Object o, boolean pretty) throws JsonProcessingException {
    if (pretty) {
      return _mapper.get().writerWithDefaultPrettyPrinter().writeValueAsString(o);
    } else {
      return _mapper.get().writeValueAsString(o);
    }
  }

  public byte[] toJSONPretty(String config) throws IOException {
    return toJSONPretty(readTree(config));
  }

  public byte[] toJSONPretty(Object config) throws JsonProcessingException {
    return _mapper.get().writerWithDefaultPrettyPrinter().writeValueAsBytes(config);
  }

  /**
   * Transforms a bean (aka POJO) to a JSONObject.
   */
  public JSONObject toJSONObject(Object o) throws JsonProcessingException, ParseException {
    return (JSONObject) _parser.get().parse(toJSON(o, false));
  }

  /**
   * Reads a JSON string into a JsonNode Object
   *
   * @param json JSON value to deserialize
   * @return deserialized JsonNode Object
   */
  public JsonNode readTree(String json) throws IOException {
    return _mapper.get().readTree(json);
  }

  /**
   * Reads a JSON byte array into a JsonNode Object
   *
   * @param json JSON value to deserialize
   * @return deserialized JsonNode Object
   */
  public JsonNode readTree(byte[] json) throws IOException {
    return _mapper.get().readTree(json);
  }

  /**
   * Update JSON given a JSON Patch (see RFC 6902 at https://tools.ietf.org/html/rfc6902)
   * Operations:
   * <ul>
   *   <li>add</li>
   *   <li>remove</li>
   *   <li>replace</li>
   *   <li>move</li>
   *   <li>copy</li>
   *   <li>test</li>
   * </ul>
   *
   * @param patch Array of JSON patches, e.g. [{ "op": "move", "from": "/a", "path": "/c" }]
   * @param source Source JSON to apply patch to
   * @return new json after applying the patch
   */
  public JsonNode applyPatch(String patch, String source) throws IOException {
    JsonNode patchNode = readTree(patch);
    JsonNode sourceNode = readTree(source);
    return applyPatch(patchNode, sourceNode);
  }

  public JsonNode applyPatch(JsonNode patch, JsonNode source) throws IOException {
    return JsonPatch.apply(patch, source);
  }

}
