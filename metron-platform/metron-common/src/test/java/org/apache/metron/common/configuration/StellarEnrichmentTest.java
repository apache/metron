package org.apache.metron.common.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
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
  public static List<String> DEFAULT_CONFIGS = ImmutableList.of(defaultStellarConfig_list, defaultStellarConfig_map);

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
  public static List<String> GROUPED_CONFIGS = ImmutableList.of(groupedStellarConfig_list, groupedStellarConfig_map);

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
    Map<String, Object> ret = JSONUtils.INSTANCE.load(message, new TypeReference<Map<String, Object>>() {
    });
    return new JSONObject(ret);
  }

}
