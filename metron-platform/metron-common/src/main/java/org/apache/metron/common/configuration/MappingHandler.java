package org.apache.metron.common.configuration;

import com.google.common.collect.ImmutableList;
import org.apache.metron.common.field.mapping.FieldMapping;
import org.apache.metron.common.field.mapping.FieldMappings;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingHandler implements Serializable {
  private List<String> input;
  private String output;
  private FieldMapping mapping;
  private Map<String, Object> config;

  public MappingHandler() {
  }

  public List<String> getInput() {
    return input;
  }

  public void setInput(Object inputFields) {
    if(inputFields instanceof String) {
      this.input= ImmutableList.of(inputFields.toString());
    }
    else if(inputFields instanceof List) {
      this.input= (List<String>)inputFields;
    }
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String outputField) {
    this.output= outputField;
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  public FieldMapping getMapping() {
    return mapping;
  }

  public void setMapping(String mapping) {
    this.mapping = FieldMappings.get(mapping);
  }

  public void initAndValidate() {
    if(getMapping() == null) {
      throw new IllegalStateException("Mapping cannot be null.");
    }
    if(output== null) {
      if(input!= null && input.size() == 1) {
        output = input.get(0);
      }
      else {
        throw new IllegalStateException("You must specify an output field");
      }
    }
  }

  public Map<String, Object> map(JSONObject input, Map<String, Object> sensorConfig) {
    if(getInput() == null || getInput().isEmpty()) {
      return mapping.map(input, getOutput(), config, sensorConfig);
    }
    else {
      Map<String, Object> in = new HashMap<>();
      for(String inputField : getInput()) {
        in.put(inputField, input.get(inputField));
      }
      return mapping.map(in, getOutput(), config, sensorConfig);
    }
  }
}
