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
package org.apache.metron.common;

import java.util.HashMap;
import java.util.Map;

public class Constants {

  public static final String ZOOKEEPER_ROOT = "/metron";
  public static final String ZOOKEEPER_TOPOLOGY_ROOT = ZOOKEEPER_ROOT + "/topology";
  public static final long DEFAULT_CONFIGURED_BOLT_TIMEOUT = 5000;
  public static final String SENSOR_TYPE = "source.type";
  public static final String ENRICHMENT_TOPIC = "enrichments";
  public static final String DEFAULT_PARSER_ERROR_TOPIC = "parser_error";
  public static final String DEFAULT_PARSER_INVALID_TOPIC = "parser_invalid";
  public static final String ERROR_STREAM = "error";
  public static final String INVALID_STREAM = "invalid";
  public static final String SIMPLE_HBASE_ENRICHMENT = "hbaseEnrichment";
  public static final String SIMPLE_HBASE_THREAT_INTEL = "hbaseThreatIntel";

  public static enum Fields {
     SRC_ADDR("ip_src_addr")
    ,SRC_PORT("ip_src_port")
    ,DST_ADDR("ip_dst_addr")
    ,DST_PORT("ip_dst_port")
    ,PROTOCOL("protocol")
    ,TIMESTAMP("timestamp")
    ,INCLUDES_REVERSE_TRAFFIC("includes_reverse_traffic")
    ;
    private static Map<String, Fields> nameToField;

    static {
      nameToField = new HashMap<>();
      for (Fields f : Fields.values()) {
        nameToField.put(f.getName(), f);
      }
    }

    private String name;

    Fields(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static Fields fromString(String fieldName) {
      return nameToField.get(fieldName);
    }
  }

}

