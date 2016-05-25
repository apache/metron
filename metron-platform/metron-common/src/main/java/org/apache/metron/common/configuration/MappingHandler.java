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

  @Override
  public String toString() {
    return "MappingHandler{" +
            "input=" + input +
            ", output='" + output + '\'' +
            ", mapping=" + mapping +
            ", config=" + config +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MappingHandler that = (MappingHandler) o;

    if (getInput() != null ? !getInput().equals(that.getInput()) : that.getInput() != null) return false;
    if (getOutput() != null ? !getOutput().equals(that.getOutput()) : that.getOutput() != null) return false;
    if (getMapping() != null ? !getMapping().equals(that.getMapping()) : that.getMapping() != null) return false;
    return getConfig() != null ? getConfig().equals(that.getConfig()) : that.getConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = getInput() != null ? getInput().hashCode() : 0;
    result = 31 * result + (getOutput() != null ? getOutput().hashCode() : 0);
    result = 31 * result + (getMapping() != null ? getMapping().hashCode() : 0);
    result = 31 * result + (getConfig() != null ? getConfig().hashCode() : 0);
    return result;
  }
}
