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

package org.apache.metron.common.query;


import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.util.List;

public enum StringFunctions implements Function<List<String>, String> {
  TO_LOWER(strings -> strings.get(0)==null?null:strings.get(0).toLowerCase())
  ,TO_UPPER(strings -> strings.get(0) == null?null:strings.get(0).toUpperCase())
  ,TRIM(strings -> strings.get(0) == null?null:strings.get(0).trim())
  ;
  Function<List<String>, String> func;
  StringFunctions(Function<List<String>, String> func) {
    this.func = func;
  }

  @Nullable
  @Override
  public String apply(@Nullable List<String> input) {
    return func.apply(input);
  }
}
