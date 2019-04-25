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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonPatch;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum JSONUtils {
  INSTANCE;

  public static class ReferenceSupplier<T> implements Supplier<TypeReference<T>> {
    Type type;
    protected ReferenceSupplier() {
      Type superClass = this.getClass().getGenericSuperclass();
      if(superClass instanceof Class) {
        throw new IllegalArgumentException("Internal error: ReferenceSupplier constructed without actual type information");
      } else {
        this.type = ((ParameterizedType)superClass).getActualTypeArguments()[0];
      }
    }

    @Override
    public TypeReference<T> get() {
      return new TypeReference<T>() {
        @Override
        public Type getType() {
          return type;
        }
      };
    }
  }

  public final static ReferenceSupplier<Map<String, Object>> MAP_SUPPLIER = new ReferenceSupplier<Map<String, Object>>() {};
  public final static ReferenceSupplier<List<Object>> LIST_SUPPLIER = new ReferenceSupplier<List<Object>>(){};

  private static ThreadLocal<JSONParser> _parser = ThreadLocal.withInitial(() ->
      new JSONParser());

  private static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .configure(JsonParser.Feature.ALLOW_COMMENTS, true));

  public <T> T convert(Object original, Class<T> targetClass) {
    return _mapper.get().convertValue(original, targetClass);
  }

  public ObjectMapper getMapper() {
    return _mapper.get();
  }


  public <T> T load(InputStream is, ReferenceSupplier<T> ref) throws IOException {
    return _mapper.get().readValue(is, (TypeReference<T>)ref.get());
  }

  public <T> T load(String is, ReferenceSupplier<T> ref) throws IOException {
    return _mapper.get().readValue(is, (TypeReference<T>)ref.get());
  }

  /**
   * Loads JSON from a file and ensures it's located in the specified class.
   *
   * @param f The file to read from
   * @param ref A {@link ReferenceSupplier} for the class to be loaded into.
   * @param <T> The type parameter of the class
   * @return The JSON loaded into the provided class
   * @throws IOException If there's an issue loading the JSON
   */
  public <T> T load(File f, ReferenceSupplier<T> ref) throws IOException {
    try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
      return _mapper.get().readValue(is, (TypeReference<T>)ref.get());
    }
  }

  public <T> T load(InputStream is, Class<T> clazz) throws IOException {
    return _mapper.get().readValue(is, clazz);
  }

  /**
   * Loads JSON from a file and ensures it's located in the provided class.
   *
   * @param f The file to read from
   * @param clazz The class to read into
   * @param <T> The type parameter of the class
   * @return The JSON loaded into the provided class
   * @throws IOException If there's an issue loading the JSON
   */
  public <T> T load(File f, Class<T> clazz) throws IOException {
    try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
      return _mapper.get().readValue(is, clazz);
    }
  }

  public <T> T load(String is, Class<T> clazz) throws IOException {
    return _mapper.get().readValue(is, clazz);
  }

  /**
   * Converts an object to a JSON string. Can be a pretty string
   *
   * @param o The object to convert
   * @param pretty Pretty formatted string if true, otherwise not pretty formatted.
   * @return A JSON string representation of the object
   * @throws JsonProcessingException If there's an issue converting to JSON.
   */
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
   * Reads a JSON string into a JsonNode Object.
   *
   * @param json JSON value to deserialize
   * @return deserialized JsonNode Object
   */
  JsonNode readTree(String json) throws IOException {
    return _mapper.get().readTree(json);
  }

  /**
   * Reads a JSON byte array into a JsonNode Object.
   *
   * @param json JSON value to deserialize
   * @return deserialized JsonNode Object
   */
  JsonNode readTree(byte[] json) throws IOException {
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
  public byte[] applyPatch(String patch, String source) throws IOException {
    JsonNode patchNode = readTree(patch);
    JsonNode sourceNode = readTree(source);
    return toJSONPretty(JsonPatch.apply(patchNode, sourceNode));
  }

  /**
   * Update JSON given a JSON Patch (see RFC 6902 at https://tools.ietf.org/html/rfc6902)
   *
   * @param patch Array of JSON patches in raw bytes
   * @param source Source JSON in raw bytes to apply patch to
   * @return new json after applying the patch
   *
   * @see JSONUtils#applyPatch(String, String)
   */
  public byte[] applyPatch(byte[] patch, byte[] source) throws IOException {
    JsonNode patchNode = readTree(patch);
    JsonNode sourceNode = readTree(source);
    return toJSONPretty(JsonPatch.apply(patchNode, sourceNode));
  }


  /**
   * Update JSON given a JSON Patch (see RFC 6902 at https://tools.ietf.org/html/rfc6902)
   *
   * @param patch List of JSON patches in map form
   * @param source Source JSON in map form to apply patch to
   * @return new json after applying the patch
   *
   * @see JSONUtils#applyPatch(String, String)
   */
  public Map<String, Object> applyPatch(List<Map<String, Object>> patch, Map<String, Object> source) {
    JsonNode originalNode = convert(source, JsonNode.class);
    JsonNode patchNode = convert(patch, JsonNode.class);
    JsonNode patched = JsonPatch.apply(patchNode, originalNode);
    return _mapper.get().convertValue(patched, new TypeReference<Map<String, Object>>() { });
  }

}
