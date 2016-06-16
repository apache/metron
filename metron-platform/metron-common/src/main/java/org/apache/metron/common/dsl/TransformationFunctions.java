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

import org.apache.metron.common.dsl.functions.DateFunctions;
import org.apache.metron.common.dsl.functions.MapFunctions;
import org.apache.metron.common.dsl.functions.NetworkFunctions;
import org.apache.metron.common.dsl.functions.StringFunctions;
import org.apache.metron.common.utils.ConversionUtils;

import java.util.List;
import java.util.function.Function;

public enum TransformationFunctions implements Function<List<Object>, Object> {
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
  ;
  Function<List<Object>, Object> func;
  TransformationFunctions(Function<List<Object>, Object> func) {
    this.func = func;
  }

  @Override
  public Object apply(List<Object> input) {
    return func.apply(input);
  }
}
