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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.configuration.enrichment.handler.Configs;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnrichmentConfigTest {
  /**
   {
    "fieldMap": {
      "geo": ["ip_dst_addr", "ip_src_addr"],
      "host": ["host"],
      "stellar" : {
         "type" : "STELLAR"
        ,"config" : {
            "foo" : "1 + 1"
           ,"ALL_CAPS" : "TO_UPPER(source.type)"
                    }
                  }
              }
   }
   */
  @Multiline
  public static String sourceConfigStr;

  @Test
  public void testSerialization() throws Exception
  {
    EnrichmentConfig config = JSONUtils.INSTANCE.load(sourceConfigStr, EnrichmentConfig.class);
    assertTrue(config.getFieldMap().get("stellar") instanceof Map);
    assertTrue(config.getEnrichmentConfigs().get("stellar") instanceof ConfigHandler);
    assertEquals(Configs.STELLAR, ((ConfigHandler)config.getEnrichmentConfigs().get("stellar")).getType());
  }


}
