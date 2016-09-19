package org.apache.metron.parsers.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONMapParser extends BasicParser {
  private static interface Handler {
    JSONObject handle(String key, Map value, JSONObject obj);
  }
  public static enum MapStrategy implements Handler {
     DROP((key, value, obj) -> obj)
    ,UNFOLD( (key, value, obj) -> {
      Set<Map.Entry<Object, Object>> entrySet = value.entrySet();
      for(Map.Entry<Object, Object> kv : entrySet) {
        String newKey = Joiner.on(".").join(key, kv.getKey().toString());
        obj.put(newKey, kv.getValue());
      }
      return obj;
    })
    ,ALLOW((key, value, obj) -> {
      obj.put(key, value);
      return obj;
    })
    ,ERROR((key, value, obj) -> {
      throw new IllegalStateException("Unable to process " + key + " => " + value + " because value is a map.");
    })
    ;
    Handler handler;
    MapStrategy(Handler handler) {
      this.handler = handler;
    }

    @Override
    public JSONObject handle(String key, Map value, JSONObject obj) {
      return handler.handle(key, value, obj);
    }

  }
  public static final String MAP_STRATEGY_CONFIG = "mapStrategy";
  private MapStrategy mapStrategy = MapStrategy.DROP;

  @Override
  public void configure(Map<String, Object> config) {
    String strategyStr = (String) config.getOrDefault(MAP_STRATEGY_CONFIG, MapStrategy.DROP.name());
    mapStrategy = MapStrategy.valueOf(strategyStr);
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
   * @param rawMessage
   * @return If null is returned, this is treated as an empty list.
   */
  @Override
  public List<JSONObject> parse(byte[] rawMessage) {
    try {
      String originalString = new String(rawMessage);
      //convert the JSON blob into a String -> Object map
      Map<String, Object> rawMap = JSONUtils.INSTANCE.load(originalString, new TypeReference<Map<String, Object>>() {
      });
      JSONObject ret = normalizeJSON(rawMap);
      ret.put("original_string", originalString );
      if(!ret.containsKey("timestamp")) {
        //we have to ensure that we have a timestamp.  This is one of the pre-requisites for the parser.
        ret.put("timestamp", System.currentTimeMillis());
      }
      return ImmutableList.of(ret);
    } catch (Throwable e) {
      String message = "Unable to parse " + new String(rawMessage) + ": " + e.getMessage();
      LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }
  }

  /**
   * Remove all collections as values.  We have standardized on one-dimensional maps as our data model..
   *
   * @param map
   * @return
   */
  private JSONObject normalizeJSON(Map<String, Object> map) {
    JSONObject ret = new JSONObject();
    for(Map.Entry<String, Object> kv : map.entrySet()) {
      if(kv.getValue() instanceof Map) {
        mapStrategy.handle(kv.getKey(), (Map) kv.getValue(), ret);
      }
      else {
        ret.put(kv.getKey(), kv.getValue());
      }
    }
    return ret;
  }

}
