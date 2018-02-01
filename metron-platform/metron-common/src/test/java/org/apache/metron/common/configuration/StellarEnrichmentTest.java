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
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StellarEnrichmentTest {
  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "stmt1" : "TO_UPPER(source.type)",
          "stmt2" : "TO_LOWER(stmt1)",
          "stmt3" : "TO_LOWER(string)"
        }
      }
    }
  }
   */
  @Multiline
  public static String defaultStellarConfig_map;

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : [
          "stmt1 := TO_UPPER(source.type)",
          "stmt2 := TO_LOWER(stmt1)",
          "stmt3 := TO_LOWER(string)"
        ]
      }
    }
  }
   */
  @Multiline
  public static String defaultStellarConfig_list;

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : [
          "stmt1 := TO_UPPER(source.type)",
          "stmt4 := TO_LOWER(string)",
          "stmt2 := TO_LOWER(stmt1)",
          "stmt3 := TO_LOWER(string)",
          "stmt4 := null"
        ]
      }
    }
  }
   */
  @Multiline
  public static String defaultStellarConfig_listWithTemp;
  public static List<String> DEFAULT_CONFIGS = ImmutableList.of(defaultStellarConfig_list, defaultStellarConfig_map, defaultStellarConfig_listWithTemp);

/**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "group1" : {
            "stmt1" : "TO_UPPER(source.type)",
            "stmt2" : "TO_LOWER(stmt1)"
          },
          "group2" : {
            "stmt3" : "TO_LOWER(string)"
          }
        }
      }
    }
  }
   */
  @Multiline
  public static String groupedStellarConfig_map;

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "group1" : [
            "stmt1 := TO_UPPER(source.type)",
            "stmt2 := TO_LOWER(stmt1)"
          ],
          "group2" : [
            "stmt3 := TO_LOWER(string)"
          ]
        }
      }
    }
  }
   */
  @Multiline
  public static String groupedStellarConfig_list;

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "group1" : [
            "stmt1 := TO_UPPER(source.type)",
            "stmt2 := TO_LOWER(stmt1)"
          ],
          "group2" : [
            "stmt3 := TO_LOWER(string)",
            "stmt4 := TO_LOWER(string)",
            "stmt4 := null"
          ]
        }
      }
    }
  }
   */
  @Multiline
  public static String groupedStellarConfig_listWithTemp;
  public static List<String> GROUPED_CONFIGS = ImmutableList.of(groupedStellarConfig_listWithTemp, groupedStellarConfig_list, groupedStellarConfig_map);

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "group1" : {
            "stmt1" : "TO_UPPER(source.type)",
            "stmt2" : "TO_LOWER(stmt1)"
          },
          "group2" : {
            "stmt3" : "TO_LOWER(string)"
          },
          "stmt4" : "1 + 1",
          "stmt5" : "FORMAT('%s', source.type)"
        }
      }
    }
  }
   */
  @Multiline
  public static String mixedStellarConfig_map;

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "group1" : [
            "stmt1 := TO_UPPER(source.type)",
            "stmt2 := TO_LOWER(stmt1)"
          ],
          "group2" : [
            "stmt3 := TO_LOWER(string)"
          ],
          "stmt4" : "1 + 1",
          "stmt5" : "FORMAT('%s', source.type)"
        }
      }
    }
  }
   */
  @Multiline
  public static String mixedStellarConfig_list;
  public static List<String> MIXED_CONFIGS = ImmutableList.of(mixedStellarConfig_list, mixedStellarConfig_map);

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "group1" : [
            "stmt1 := TO_UPPER(source.type)",
            "stmt2 := TO_LOWER(stmt1)",
            "stmt1 := null"
          ],
          "group2" : [
            "stmt3 := TO_LOWER(string)"
          ],
          "stmt4" : "1 + 1",
          "stmt5" : "FORMAT('%s', source.type)"
        }
      }
    }
  }
   */
  @Multiline
  public static String tempVarStellarConfig_list;

  /**
   {
    "string" : "foo"
   ,"number" : 2
   ,"source.type" : "stellar_test"
   }
   */
  @Multiline
  public static String message;

  public static JSONObject getMessage() throws IOException {
    Map<String, Object> ret = JSONUtils.INSTANCE.load(message, JSONUtils.MAP_SUPPLIER);
    return new JSONObject(ret);
  }

}
