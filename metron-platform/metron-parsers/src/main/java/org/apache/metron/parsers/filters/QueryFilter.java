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

package org.apache.metron.parsers.filters;

import com.google.common.collect.ImmutableList;
import org.apache.metron.common.query.MapVariableResolver;
import org.apache.metron.common.query.PredicateProcessor;
import org.apache.metron.common.query.VariableResolver;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryFilter implements MessageFilter<JSONObject> {
  public static final String QUERY_STRING_CONF = "filter.query";
  private PredicateProcessor processor = new PredicateProcessor();
  private String query;

  public QueryFilter()
  {

  }

  @Override
  public void configure(Map<String, Object> config) {
    Object o = config.get(QUERY_STRING_CONF);
    if(o instanceof String) {
      query= o.toString();
    }

    processor.validate(query, true);
  }

  @Override
  public boolean emitTuple(JSONObject message) {
    VariableResolver resolver = new MapVariableResolver(message);
    return processor.parse(query, resolver);
  }
}
