package org.apache.metron.common.field.mapping;

import com.google.common.collect.Iterables;

import java.util.Map;

public abstract class SimpleFieldMapping implements FieldMapping {
  @Override
  public Map<String, Object> map (Map<String, Object> input
                                , String outputField
                                , Map<String, Object> fieldMappingConfig
                                , Map<String, Object> sensorConfig
                                )
  {
    Object value = (input == null || input.values() == null && input.values().isEmpty())
                 ? null
                 : Iterables.getFirst(input.values(), null)
                 ;
    return map(value, outputField);
  }

  public abstract Map<String, Object> map(Object input, String outputField);
}
