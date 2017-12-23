/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.utils.validation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarConfiguration;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarConfigurationList;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarExpressionField;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarExpressionList;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarExpressionMap;

@StellarConfiguration
@SuppressWarnings("unchecked")
public class TestConfigObject {

  // a field
  @StellarExpressionField(name = "the field")
  private String stellar = "TO_UPPER('stellarexpressionfield')";

  // a list of stellar
  @StellarExpressionList(name = "the list of stellar strings")
  private List<String> stellarList = new LinkedList<String>() {{
    add("TO_UPPER('list stellar item one')");
    add("TO_UPPER('list stellar item two')");
  }};


  // a map
  @StellarExpressionMap(name = "plain old map")
  private Map<String, String> plainMap = new HashMap() {{
    put("stellar_map_item", "TO_UPPER('SMI')");
  }};


  // a map with the 'real' map burried 2 layers in
  @StellarExpressionMap(name = "map two deep", innerMapKeys = {"level1", "level2"})
  private Map<String, Object> mapTwoDeep = new HashMap(){{
    put("level1",new HashMap(){{
      put("level2",new HashMap(){{
        put("nested stellar key", "TO_UPPER('dig down deep')");
      }});
    }});
  }};

  @StellarExpressionMap(name = "map depending on field type", qualifyWithField = "qualifier_field", qualifyWithFieldType = java.lang.Integer.class)
  private Map<String, Object> dependsMap = new HashMap(){{
    put("stellar_key", "TO_UPPER('it was int')");
  }};

  @StellarExpressionMap(name = "map depending on field type but wrong", qualifyWithField = "qualifier_field", qualifyWithFieldType = java.lang.String.class)
  private Map<String, Object> dependsButNotMap = new HashMap(){{
    put("stellar_key", "TO_UPPER('it was not int')");
  }};

  private Object qualifier_field = new Integer(2);

  @StellarConfiguration
  private TestChildConfigObject child = new TestChildConfigObject(1);

  @StellarConfigurationList(name = "list of holders")
  private List stellarConfigList = new LinkedList(){{
    add(new TestChildConfigObject(2));
    add(new TestChildConfigObject(3));
  }};
}
