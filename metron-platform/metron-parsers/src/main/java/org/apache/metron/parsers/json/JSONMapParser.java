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

package org.apache.metron.parsers.json;

import com.google.common.base.Joiner;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.cache.CacheProvider;
import com.jayway.jsonpath.spi.cache.LRUCache;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;

public class JSONMapParser extends BasicParser {

  private interface Handler {

    JSONObject handle(String key, Map value, JSONObject obj);
  }

  @SuppressWarnings("unchecked")
  public enum MapStrategy implements Handler {
    DROP((key, value, obj) -> obj), UNFOLD((key, value, obj) -> {
      return recursiveUnfold(key, value, obj);
    }), ALLOW((key, value, obj) -> {
      obj.put(key, value);
      return obj;
    }), ERROR((key, value, obj) -> {
      throw new IllegalStateException(
          "Unable to process " + key + " => " + value + " because value is a map.");
    });
    Handler handler;

    MapStrategy(Handler handler) {
      this.handler = handler;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject recursiveUnfold(String key, Map value, JSONObject obj) {
      Set<Map.Entry<Object, Object>> entrySet = value.entrySet();
      for (Map.Entry<Object, Object> kv : entrySet) {
        String newKey = Joiner.on(".").join(key, kv.getKey().toString());
        if (kv.getValue() instanceof Map) {
          recursiveUnfold(newKey, (Map) kv.getValue(), obj);
        } else {
          obj.put(newKey, kv.getValue());
        }
      }
      return obj;
    }

    @Override
    public JSONObject handle(String key, Map value, JSONObject obj) {
      return handler.handle(key, value, obj);
    }

  }

  public static final String MAP_STRATEGY_CONFIG = "mapStrategy";
  public static final String JSONP_QUERY = "jsonpQuery";

  private MapStrategy mapStrategy = MapStrategy.DROP;
  private transient TypeRef<List<Map<String, Object>>> typeRef = null;
  private String jsonpQuery = null;


  @Override
  public void configure(Map<String, Object> config) {
    String strategyStr = (String) config.getOrDefault(MAP_STRATEGY_CONFIG, MapStrategy.DROP.name());
    mapStrategy = MapStrategy.valueOf(strategyStr);
    if (config.containsKey(JSONP_QUERY)) {
      typeRef = new TypeRef<List<Map<String, Object>>>() { };
      jsonpQuery = (String) config.get(JSONP_QUERY);
      Configuration.setDefaults(new Configuration.Defaults() {

        private final JsonProvider jsonProvider = new JacksonJsonProvider();
        private final MappingProvider mappingProvider = new JacksonMappingProvider();

        @Override
        public JsonProvider jsonProvider() {
          return jsonProvider;
        }

        @Override
        public MappingProvider mappingProvider() {
          return mappingProvider;
        }

        @Override
        public Set<Option> options() {
          return EnumSet.of(Option.SUPPRESS_EXCEPTIONS);
        }
      });

      if (CacheProvider.getCache() == null) {
        CacheProvider.setCache(new LRUCache(100));
      }
    }
  }

  /**
   * Initialize the message parser.  This is done once.
   */
  @Override
  public void init() {

  }

  /**
   * Take raw data and convert it to a list of messages.
   *
   * @return If null is returned, this is treated as an empty list.
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<JSONObject> parse(byte[] rawMessage) {
    try {
      String originalString = new String(rawMessage);
      List<Map<String, Object>> messages = new ArrayList<>();

      if (!StringUtils.isEmpty(jsonpQuery)) {
        Object parsedObject = JsonPath.parse(new String(rawMessage)).read(jsonpQuery, typeRef);
        if(parsedObject != null) {
          messages.addAll((List<Map<String,Object>>)parsedObject);
        }
      } else {
        messages.add(JSONUtils.INSTANCE.load(originalString, JSONUtils.MAP_SUPPLIER));
      }

      ArrayList<JSONObject> parsedMessages = new ArrayList<>();
      for (Map<String, Object> rawMessageMap : messages) {
        JSONObject originalJsonObject = new JSONObject(rawMessageMap);
        JSONObject ret = normalizeJson(rawMessageMap);
        // the original string is the original for THIS sub message
        ret.put("original_string", originalJsonObject.toJSONString());
        if (!ret.containsKey("timestamp")) {
          ret.put("timestamp", System.currentTimeMillis());
        }
        parsedMessages.add(ret);
      }
      return Collections.unmodifiableList(parsedMessages);
    } catch (Throwable e) {
      String message = "Unable to parse " + new String(rawMessage) + ": " + e.getMessage();
      LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }
  }

  /**
   * Process all sub-maps via the MapHandler.
   * We have standardized on one-dimensional maps as our data model.
   */
  @SuppressWarnings("unchecked")
  private JSONObject normalizeJson(Map<String, Object> map) {
    JSONObject ret = new JSONObject();
    for (Map.Entry<String, Object> kv : map.entrySet()) {
      if (kv.getValue() instanceof Map) {
        mapStrategy.handle(kv.getKey(), (Map) kv.getValue(), ret);
      } else {
        ret.put(kv.getKey(), kv.getValue());
      }
    }
    return ret;
  }

}
