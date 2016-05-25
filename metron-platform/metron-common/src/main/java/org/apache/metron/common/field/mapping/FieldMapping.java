package org.apache.metron.common.field.mapping;

import java.util.Map;

public interface FieldMapping {
  Map<String, Object> map( Map<String, Object> input
                         , String outputField
                         , Map<String, Object> fieldMappingConfig
                         , Map<String, Object> sensorConfig
                         );
}
