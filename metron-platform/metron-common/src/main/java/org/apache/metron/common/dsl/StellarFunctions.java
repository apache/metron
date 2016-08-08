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

package org.apache.metron.common.dsl;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.metron.common.dsl.functions.DateFunctions;
import org.apache.metron.common.dsl.functions.MapFunctions;
import org.apache.metron.common.dsl.functions.NetworkFunctions;
import org.apache.metron.common.dsl.functions.StringFunctions;
import org.apache.metron.common.field.transformation.IPProtocolTransformation;
import org.apache.metron.common.field.validation.network.DomainValidation;
import org.apache.metron.common.field.validation.network.EmailValidation;
import org.apache.metron.common.field.validation.network.IPValidation;
import org.apache.metron.common.field.validation.network.URLValidation;
import org.apache.metron.common.field.validation.primitive.DateValidation;
import org.apache.metron.common.field.validation.primitive.IntegerValidation;
import org.apache.metron.common.utils.ConversionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public enum StellarFunctions implements Function<List<Object>, Object> {
  TO_LOWER(strings -> strings.get(0)==null?null:strings.get(0).toString().toLowerCase())
  ,TO_UPPER(strings -> strings.get(0) == null?null:strings.get(0).toString().toUpperCase())
  ,TO_STRING(strings -> strings.get(0) == null?null:strings.get(0).toString())
  ,TO_INTEGER(strings -> strings.get(0) == null?null: ConversionUtils.convert(strings.get(0), Integer.class))
  ,TO_DOUBLE(strings -> strings.get(0) == null?null: ConversionUtils.convert(strings.get(0), Double.class))
  ,TRIM(strings -> strings.get(0) == null?null:strings.get(0).toString().trim())
  ,JOIN(new StringFunctions.JoinFunction())
  ,SPLIT(new StringFunctions.SplitFunction())
  ,GET_FIRST(new StringFunctions.GetFirst())
  ,GET_LAST(new StringFunctions.GetLast())
  ,GET(new StringFunctions.Get())
  ,MAP_GET(new MapFunctions.MapGet())
  ,DOMAIN_TO_TLD(new NetworkFunctions.ExtractTLD())
  ,DOMAIN_REMOVE_TLD(new NetworkFunctions.RemoveTLD())
  ,DOMAIN_REMOVE_SUBDOMAINS(new NetworkFunctions.RemoveSubdomains())
  ,URL_TO_HOST(new NetworkFunctions.URLToHost())
  ,URL_TO_PORT(new NetworkFunctions.URLToPort())
  ,URL_TO_PATH(new NetworkFunctions.URLToPath())
  ,URL_TO_PROTOCOL(new NetworkFunctions.URLToProtocol())
  ,TO_EPOCH_TIMESTAMP(new DateFunctions.ToTimestamp())
  ,PROTOCOL_TO_NAME(new IPProtocolTransformation())
  ,IS_EMPTY ( list -> {
    if(list.size() == 0) {
      throw new IllegalStateException("IS_EMPTY expects one string arg");
    }
    String val = (String) list.get(0);
    return val == null || val.isEmpty() ? true:false;
  })
  ,IN_SUBNET( list -> {
    if(list.size() < 2) {
      throw new IllegalStateException("IN_SUBNET expects at least two args: [ip, cidr1, cidr2, ...]"
                                     + " where cidr is the subnet mask in cidr form"
                                     );
    }
    String ip = (String) list.get(0);
    if(ip == null) {
      return false;
    }
    boolean inSubnet = false;
    for(int i = 1;i < list.size() && !inSubnet;++i) {
      String cidr = (String) list.get(1);
      if(cidr == null) {
        continue;
      }
      inSubnet |= new SubnetUtils(cidr).getInfo().isInRange(ip);
    }

    return inSubnet;
  })
  ,STARTS_WITH( list -> {
    if(list.size() < 2) {
      throw new IllegalStateException("STARTS_WITH expects two args: [string, prefix] where prefix is the string fragment that the string should start with");
    }
    String prefix = (String) list.get(1);
    String str = (String) list.get(0);
    if(str == null || prefix == null) {
      return false;
    }
    return str.startsWith(prefix);
  })
  ,ENDS_WITH( list -> {
    if(list.size() < 2) {
      throw new IllegalStateException("ENDS_WITH expects two args: [string, suffix] where suffix is the string fragment that the string should end with");
    }
    String prefix = (String) list.get(1);
    String str = (String) list.get(0);
    if(str == null || prefix == null) {
      return false;
    }
    return str.endsWith(prefix);
  })
  ,REGEXP_MATCH( list -> {
     if(list.size() < 2) {
      throw new IllegalStateException("REGEXP_MATCH expects two args: [string, pattern] where pattern is a regexp pattern");
    }
    String pattern = (String) list.get(1);
    String str = (String) list.get(0);
    if(str == null || pattern == null) {
      return false;
    }
    return str.matches(pattern);
  })
  , IS_IP(new Predicate2Transformation(new IPValidation()))
  , IS_DOMAIN(new Predicate2Transformation(new DomainValidation()))
  , IS_EMAIL(new Predicate2Transformation(new EmailValidation()))
  , IS_URL(new Predicate2Transformation(new URLValidation()))
  , IS_DATE(new Predicate2Transformation(new DateValidation()))
  , IS_INTEGER(new Predicate2Transformation(new IntegerValidation()))
  , MAP_EXISTS(list -> {
      if(list.size() < 2) {
        return false;
      }
      Object key = list.get(0);
      Object mapObj = list.get(1);
      if(key != null && mapObj != null && mapObj instanceof Map) {
        return ((Map)mapObj).containsKey(key);
      }
      return false;
    }
  )
  ;
  private static class Predicate2Transformation implements Function<List<Object>, Object> {
    Predicate<List<Object>> pred;
    public Predicate2Transformation(Predicate<List<Object>> pred) {
      this.pred = pred;
    }

    @Override
    public Object apply(List<Object> objects) {
      return pred.test(objects);
    }
  }
  Function<List<Object>, Object> func;
  StellarFunctions(Function<List<Object>, Object> func) {
    this.func = func;
  }



  @Override
  public Object apply(List<Object> input) {
    return func.apply(input);
  }
}
