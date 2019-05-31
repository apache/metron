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

package org.apache.metron.stellar.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


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

  public final static ReferenceSupplier<Map<String, Object>> MAP_SUPPLIER = new ReferenceSupplier<Map<String, Object>>(){};
  public final static ReferenceSupplier<List<Object>> LIST_SUPPLIER = new ReferenceSupplier<List<Object>>(){};

  private static ThreadLocal<JSONParser> _parser = ThreadLocal.withInitial(() ->
          new JSONParser());

  private static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
          new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  public <T> T load(InputStream is, ReferenceSupplier<T> ref) throws IOException {
    return _mapper.get().readValue(is, (TypeReference<T>)ref.get());
  }

  public <T> T load(String is, ReferenceSupplier<T> ref) throws IOException {
    return _mapper.get().readValue(is, (TypeReference<T>)ref.get());
  }

  public <T> T load(File f, ReferenceSupplier<T> ref) throws IOException {
    try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
      return _mapper.get().readValue(is, (TypeReference<T>)ref.get());
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

  public byte[] toJSON(Object config) throws JsonProcessingException {
    return _mapper.get().writeValueAsBytes(config);
  }

  /**
   * Transforms a bean (aka POJO) to a JSONObject.
   */
  public JSONObject toJSONObject(Object o) throws JsonProcessingException, ParseException {
    return toJSONObject(toJSON(o, false));
  }

  public JSONObject toJSONObject(String json) throws ParseException {
    return (JSONObject) _parser.get().parse(json);
  }
}
