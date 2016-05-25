package org.apache.metron.common.field.mapping;

import org.apache.metron.common.utils.ReflectionUtils;

public enum FieldMappings {
  IP_PROTOCOL(new IPMapping())
  ;
  FieldMapping mapping;
  FieldMappings(FieldMapping mapping) {
    this.mapping = mapping;
  }
  public static FieldMapping get(String mapping) {
    try {
      return FieldMappings.valueOf(mapping).mapping;
    }
    catch(Exception ex) {
      return ReflectionUtils.createInstance(mapping);
    }
  }
}
