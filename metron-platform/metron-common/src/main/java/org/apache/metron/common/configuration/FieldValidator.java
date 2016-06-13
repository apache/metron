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
import org.apache.metron.common.field.validation.FieldValidation;
import org.apache.metron.common.field.validation.FieldValidations;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldValidator {

  public enum Config {
     FIELD_VALIDATIONS("fieldValidations")
    ,VALIDATION("validation")
    ,INPUT("input")
    ,CONFIG("config")
    ;
    String key;
    Config(String key) {
      this.key = key;
    }
    public <T> T get(Map<String, Object> config, Class<T> clazz) {
      Object o = config.get(key);
      if(o == null) {
        return null;
      }
      return clazz.cast(o);
    }
  }
  private FieldValidation validation;
  private List<String> input;
  private Map<String, Object> config;

  public FieldValidator(Object o) {
    if(o instanceof Map) {
      Map<String, Object> validatorConfig = (Map<String, Object>) o;
      Object inputObj = Config.INPUT.get(validatorConfig, Object.class);
      if(inputObj instanceof String) {
        input = ImmutableList.of(inputObj.toString());
      }
      else if(inputObj instanceof List) {
        input = new ArrayList<>();
        for(Object inputO : (List<Object>)inputObj) {
          input.add(inputO.toString());
        }
      }
      config = Config.CONFIG.get(validatorConfig, Map.class);
      if(config == null) {
        config = new HashMap<>();
      }
      String validator = Config.VALIDATION.get(validatorConfig, String.class);
      if(validator == null) {
        throw new IllegalStateException("Validation not set.");
      }
      validation= FieldValidations.get(validator);
    }
    else {
      throw new IllegalStateException("Unable to configure field validations");
    }
  }

  public FieldValidation getValidation() {
    return validation;
  }

  public List<String> getInput() {
    return input;
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public boolean isValid(JSONObject inputData, Map<String, Object> globalConfig) {
    Map<String, Object> in = inputData;
    if(input != null && !input.isEmpty()) {
      in = new HashMap<>();
      for(String i : input) {
        Object o = inputData.get(i);
        in.put(i,o);
      }
    }
    return validation.isValid(in, config, globalConfig);
  }

  public static List<FieldValidator> readValidations(Map<String, Object> globalConfig) {
    List<FieldValidator> validators = new ArrayList<>();
    List<Object> validations = (List<Object>) Config.FIELD_VALIDATIONS.get(globalConfig, List.class);
    if(validations != null) {
      for (Object o : validations) {
        FieldValidator f = new FieldValidator(o);
        f.getValidation().initialize(f.getConfig(), globalConfig);
        validators.add(new FieldValidator(o));
      }
    }
    return validators;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FieldValidator that = (FieldValidator) o;

    if (getValidation() != null ? !getValidation().equals(that.getValidation()) : that.getValidation() != null)
      return false;
    if (getInput() != null ? !getInput().equals(that.getInput()) : that.getInput() != null) return false;
    return getConfig() != null ? getConfig().equals(that.getConfig()) : that.getConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = getValidation() != null ? getValidation().hashCode() : 0;
    result = 31 * result + (getInput() != null ? getInput().hashCode() : 0);
    result = 31 * result + (getConfig() != null ? getConfig().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FieldValidator{" +
            "validation=" + validation +
            ", input=" + input +
            ", config=" + config +
            '}';
  }

}
