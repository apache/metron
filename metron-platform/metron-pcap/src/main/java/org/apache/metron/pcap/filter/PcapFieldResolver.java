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

package org.apache.metron.pcap.filter;

import com.google.common.collect.ImmutableList;
import org.apache.metron.stellar.common.utils.ConcatMap;
import org.apache.metron.stellar.dsl.VariableResolver;

import java.util.HashMap;
import java.util.Map;

public class PcapFieldResolver implements VariableResolver {
  Map<String, Object> fieldsMap = new HashMap<>();

  public PcapFieldResolver(Map<String, Object> fieldsMap) {
    this.fieldsMap = fieldsMap;
  }

  @Override
  public Object resolve(String variable) {
    if(variable.equals(VariableResolver.ALL_FIELDS)) {
      return new ConcatMap(ImmutableList.of(fieldsMap));
    }
    return fieldsMap.get(variable);
  }

  @Override
  public boolean exists(String variable) {
    return fieldsMap.containsKey(variable);
  }

}
