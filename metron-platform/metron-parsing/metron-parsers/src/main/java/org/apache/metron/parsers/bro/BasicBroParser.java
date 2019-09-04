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

package org.apache.metron.parsers.bro;

import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.LazyLogger;
import org.apache.metron.common.utils.LazyLoggerFactory;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class BasicBroParser extends BasicParser {

  protected static final LazyLogger _LOG = LazyLoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final ThreadLocal<NumberFormat> DECIMAL_FORMAT = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      return new DecimalFormat("0.0#####");
    }
  };
  private JSONCleaner cleaner = new JSONCleaner();

  @Override
  public void configure(Map<String, Object> parserConfig) {
    setReadCharset(parserConfig);
  }

  @Override
  public void init() {

  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JSONObject> parse(byte[] msg) {

    _LOG.trace("[Metron] Starting to parse incoming message");

    String rawMessage = null;
    List<JSONObject> messages = new ArrayList<>();
    try {
      rawMessage = new String(msg, getReadCharset());
      _LOG.trace("[Metron] Received message: {}", rawMessage);

      JSONObject cleanedMessage = cleaner.clean(rawMessage);
      _LOG.debug("[Metron] Cleaned message: {}", cleanedMessage);

      if (cleanedMessage == null || cleanedMessage.isEmpty()) {
        throw new Exception("Unable to clean message: " + rawMessage);
      }

      String key;
      JSONObject payload;
      if (cleanedMessage.containsKey("type")) {
        key = cleanedMessage.get("type").toString();
        payload = cleanedMessage;
      } else {
        key = cleanedMessage.keySet().iterator().next().toString();

        if (key == null) {
          throw new Exception("Unable to retrieve key for message: "
                  + rawMessage);
        }

        payload = (JSONObject) cleanedMessage.get(key);
      }

      if (payload == null) {
        throw new Exception("Unable to retrieve payload for message: "
                + rawMessage);
      }

      String originalString = key.toUpperCase() + " |";
      for (Object k : payload.keySet()) {
        Object raw = payload.get(k);
        String value = raw.toString();
        if (raw instanceof Double) {
          value = DECIMAL_FORMAT.get().format(raw);
        }
        originalString += " " + k.toString() + ":" + value;
      }
      payload.put("original_string", originalString);

      replaceKey(payload, Constants.Fields.TIMESTAMP.getName(), new String[]{ "ts" });

      long timestamp = 0L;
      if (payload.containsKey(Constants.Fields.TIMESTAMP.getName())) {
        try {
          Double broTimestamp = ((Number) payload.get(Constants.Fields.TIMESTAMP.getName())).doubleValue();
          String broTimestampFormatted = DECIMAL_FORMAT.get().format(broTimestamp);
          timestamp = convertToMillis(broTimestamp);
          payload.put(Constants.Fields.TIMESTAMP.getName(), timestamp);
          payload.put("bro_timestamp", broTimestampFormatted);
          _LOG.trace("[Metron] new bro record - timestamp : {}", () -> payload.get(Constants.Fields.TIMESTAMP.getName()));
        } catch (NumberFormatException nfe) {
          _LOG.error("[Metron] timestamp is invalid: {}", payload.get("timestamp"));
          payload.put(Constants.Fields.TIMESTAMP.getName(), 0);
        }
      }

      boolean ipSrcReplaced = replaceKey(payload, Constants.Fields.SRC_ADDR.getName(), new String[]{"source_ip", "id.orig_h"});
      if (!ipSrcReplaced) {
        replaceKeyArray(payload, Constants.Fields.SRC_ADDR.getName(), new String[]{ "tx_hosts" });
      }

      boolean ipDstReplaced = replaceKey(payload, Constants.Fields.DST_ADDR.getName(), new String[]{"dest_ip", "id.resp_h"});
      if (!ipDstReplaced) {
        replaceKeyArray(payload, Constants.Fields.DST_ADDR.getName(), new String[]{ "rx_hosts" });
      }

      replaceKey(payload, Constants.Fields.SRC_PORT.getName(), new String[]{"source_port", "id.orig_p"});
      replaceKey(payload, Constants.Fields.DST_PORT.getName(), new String[]{"dest_port", "id.resp_p"});

      payload.put(Constants.Fields.PROTOCOL.getName(), key);
      _LOG.debug("[Metron] Returning parsed message: {}", payload);
      messages.add(payload);
      return messages;

    } catch (Exception e) {
      String message = "Unable to parse Message: " + rawMessage;
      _LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }

  }

  private Long convertToMillis(Double timestampSeconds) {
    return ((Double) (timestampSeconds * 1000)).longValue();
  }

  private boolean replaceKey(JSONObject payload, String toKey, String[] fromKeys) {
    for (String fromKey : fromKeys) {
      if (payload.containsKey(fromKey)) {
        Object value = payload.remove(fromKey);
        payload.put(toKey, value);
        _LOG.trace("[Metron] Added {} to {}", toKey, payload);
        return true;
      }
    }
    return false;
  }

  private boolean replaceKeyArray(JSONObject payload, String toKey, String[] fromKeys) {
    for (String fromKey : fromKeys) {
      if (payload.containsKey(fromKey)) {
        JSONArray value = (JSONArray) payload.remove(fromKey);
        if (value != null && !value.isEmpty()) {
          payload.put(toKey, value.get(0));
          _LOG.trace("[Metron] Added {} to {}", toKey, payload);
          return true;
        }
      }
    }
    return false;
  }

}
