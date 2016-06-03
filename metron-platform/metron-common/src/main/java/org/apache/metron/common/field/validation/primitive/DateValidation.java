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

package org.apache.metron.common.field.validation.primitive;

import org.apache.metron.common.field.validation.FieldValidation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class DateValidation implements FieldValidation, Predicate<List<Object>> {

  /**
   * Evaluates this predicate on the given argument.
   *
   * @param strings the input argument
   * @return {@code true} if the input argument matches the predicate,
   * otherwise {@code false}
   */
  @Override
  public boolean test(List<Object> strings) {
    if(strings.isEmpty()) {
      return false;
    }
    if(strings.size() >= 2) {
      Object date = strings.get(0);
      Object format = strings.get(1);
      if(date == null || format == null) {
        return false;
      }
      try {
        SimpleDateFormat sdf = new SimpleDateFormat(format.toString());
        sdf.parse(date.toString());
        return true;
      }
      catch(ParseException pe) {
        return false;
      }
    }
    else {
      return false;
    }
  }

  private enum Config {
    FORMAT("format")
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
  @Override
  public boolean isValid( Map<String, Object> input
                        , Map<String, Object> validationConfig
                        , Map<String, Object> globalConfig
                        )
  {
    String format = Config.FORMAT.get(validationConfig, String.class);
    if(format == null) {
      return false;
    }
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    for(Object o : input.values()) {
      if(o == null) {
        return true;
      }
      try {
        Date d = sdf.parse(o.toString());
      } catch (ParseException e) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void initialize(Map<String, Object> validationConfig, Map<String, Object> globalConfig) {
    String format = Config.FORMAT.get(validationConfig, String.class);
    if(format == null) {
      throw new IllegalStateException("You must specify '" + Config.FORMAT.key + "' in the config");
    }
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    try {
      sdf.format(new Date());
    }
    catch(Exception e) {
      throw new IllegalStateException("Invalid date format: " + format, e);
    }
  }
}
