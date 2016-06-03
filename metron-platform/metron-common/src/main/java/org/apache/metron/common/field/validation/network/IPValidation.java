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

package org.apache.metron.common.field.validation.network;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.metron.common.field.validation.FieldValidation;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class IPValidation implements FieldValidation, Predicate<List<Object>> {


  private enum IPType {
     IPV4(ip -> InetAddressValidator.getInstance().isValidInet4Address(ip))
    ,IPV6(ip -> InetAddressValidator.getInstance().isValidInet6Address(ip))
    , DEFAULT(ip -> InetAddressValidator.getInstance().isValid(ip));
    Predicate<String> validationPredicate;
    IPType(Predicate<String> validationPredicate) {
      this.validationPredicate = validationPredicate;
    }
    public boolean isValid(String ip) {
      return validationPredicate.test(ip);
    }
    public static IPType get(String type) {
      if(type == null) {
        return DEFAULT;
      }
      else {
        try {
          return IPType.valueOf(type);
        }
        catch(Exception e) {
          return DEFAULT;
        }
      }
    }
  }
  private enum Config {
    TYPE("type")
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

  /**
   * Evaluates this predicate on the given argument.
   *
   * @param strings the input argument
   * @return {@code true} if the input argument matches the predicate,
   * otherwise {@code false}
   */
  @Override
  public boolean test(List<Object> strings) {
    IPType type = IPType.DEFAULT;
    if(strings.isEmpty()) {
      return false;
    }
    Object ip =  strings.get(0);
    if(ip == null) {
      return false;
    }
    if(strings.size() >= 2) {
      Object ipType = strings.get(1);
      if(ipType != null )
      {
        try {
          type = IPType.get(ipType.toString());
        } catch (Exception e) {
          type = IPType.DEFAULT;
        }
      }
    }
    return type.isValid(ip.toString());
  }
  @Override
  public boolean isValid( Map<String, Object> input
                        , Map<String, Object> validationConfig
                        , Map<String, Object> globalConfig
                        ) {
    IPType type = IPType.get(Config.TYPE.get(validationConfig, String.class));
    for(Object o : input.values()) {
      if(o != null && !type.isValid(o.toString())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void initialize(Map<String, Object> validationConfig, Map<String, Object> globalConfig) {
  }
}
