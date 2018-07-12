package org.apache.metron.pcap;

import java.util.Map;
import java.util.function.BiFunction;

public interface ConfigOption {
  String getKey();
  default BiFunction<String, Object, Object> transform() {
    return (s,o) -> o;
  }

  default void put(Map<String, Object> map, Object value) {
    map.put(getKey(), value);
  }

  default <T> T get(Map<String, Object> map, Class<T> clazz) {
    return clazz.cast(map.get(getKey()));
  }

  default <T> T get(Map<String, Object> map, BiFunction<String, Object, T> transform, Class<T> clazz) {
    return clazz.cast(map.get(getKey()));
  }

  default <T> T getTransformed(Map<String, Object> map, Class<T> clazz) {
    return clazz.cast(transform().apply(getKey(), map.get(getKey())));
  }
}
