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
package org.apache.metron.common.configuration.enrichment.handler;

import com.google.common.collect.ImmutableList;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ListConfig implements Config {

  @Override
  public List<JSONObject> splitByFields( JSONObject message
                                 , Object fieldsObj
                                 , Function<String, String> fieldToEnrichmentKey
                                 , Iterable<Map.Entry<String, Object>> config
                                 )
  {
    List<String> fields = (List<String>)fieldsObj;
    JSONObject enrichmentObject = new JSONObject();
    if (fields != null && fields.size() > 0) {
      for (String field : fields) {
        enrichmentObject.put(fieldToEnrichmentKey.apply(field), message.get(field));
      }

    }
    return ImmutableList.of(enrichmentObject);
  }

  @Override
  public List<String> getSubgroups(Iterable<Map.Entry<String, Object>> config) {
    return ImmutableList.of("");
  }

  @Override
  public Iterable<Map.Entry<String, Object>> toConfig(Object c) {
    if(c instanceof Map) {
      return ((Map<String, Object>)c).entrySet();
    }
    else {
      return new ArrayList<>();
    }
  }
}
