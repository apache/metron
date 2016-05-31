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
import org.apache.metron.common.field.transformation.FieldTransformation;
import org.apache.metron.common.field.transformation.FieldTransformations;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldTransformer implements Serializable {
  private List<String> input = new ArrayList<>();
  private List<String> output;
  private FieldTransformation transformation;
  private Map<String, Object> config = new HashMap<>();

  public FieldTransformer() {
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

  public List<String> getOutput() {
    return output;
  }

  public void setOutput(Object outputField) {
    if(outputField instanceof String) {
      this.output = ImmutableList.of(outputField.toString());
    }
    else if(outputField instanceof List) {
      this.output = (List<String>)outputField;
    }
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  public FieldTransformation getTransformation() {
    return transformation;
  }

  public void setTransformation(String transformation) {
    this.transformation = FieldTransformations.get(transformation);
  }

  public void initAndValidate() {
    if(getTransformation() == null) {
      throw new IllegalStateException("Mapping cannot be null.");
    }

    if(output== null || output.isEmpty()) {
      if(input == null || input.isEmpty()) {
        throw new IllegalStateException("You must specify an input field if you want to leave the output fields empty");
      }
      else {
        output = input;
      }
    }
  }

  public Map<String, Object> transform(JSONObject input, Map<String, Object> sensorConfig) {
    if(getInput() == null || getInput().isEmpty()) {
      return transformation.map(input, getOutput(), config, sensorConfig);
    }
    else {
      Map<String, Object> in = new HashMap<>();
      for(String inputField : getInput()) {
        in.put(inputField, input.get(inputField));
      }
      return transformation.map(in, getOutput(), config, sensorConfig);
    }
  }

  public void transformAndUpdate(JSONObject message, Map<String, Object> sensorConfig) {
    Map<String, Object> currentValue = transform(message, sensorConfig);
    if(currentValue != null) {
      for(Map.Entry<String, Object> kv : currentValue.entrySet()) {
        if(kv.getValue() == null) {
          message.remove(kv.getKey());
        }
        else {
          message.put(kv.getKey(), kv.getValue());
        }
      }
    }
  }

  @Override
  public String toString() {
    return "MappingHandler{" +
            "input=" + input +
            ", output='" + output + '\'' +
            ", transformation=" + transformation +
            ", config=" + config +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FieldTransformer that = (FieldTransformer) o;

    if (getInput() != null ? !getInput().equals(that.getInput()) : that.getInput() != null) return false;
    if (getOutput() != null ? !getOutput().equals(that.getOutput()) : that.getOutput() != null) return false;
    if (getTransformation() != null ? !getTransformation().equals(that.getTransformation()) : that.getTransformation() != null) return false;
    return getConfig() != null ? getConfig().equals(that.getConfig()) : that.getConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = getInput() != null ? getInput().hashCode() : 0;
    result = 31 * result + (getOutput() != null ? getOutput().hashCode() : 0);
    result = 31 * result + (getTransformation() != null ? getTransformation().hashCode() : 0);
    result = 31 * result + (getConfig() != null ? getConfig().hashCode() : 0);
    return result;
  }
}
