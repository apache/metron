/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.enrichment.adapters.host;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class HostFromJSONListAdapterTest {


    private String sample_known_hosts = "[{\"ip\":\"10.1.128.236\", \"local\":\"YES\", \"type\":\"webserver\", \"asset_value\" : \"important\"},\n" +
            "{\"ip\":\"10.1.128.237\", \"local\":\"UNKNOWN\", \"type\":\"unknown\", \"asset_value\" : \"important\"},\n" +
            "{\"ip\":\"10.60.10.254\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"},\n" +
            "{\"ip\":\"10.0.2.15\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"}]";
    private String ip = "10.0.2.15";
    private String sampleMessage = "{\"known_info.local\":\"YES\", \"known_info.type\":\"printer\", \"known_info.asset_value\" : \"important\"}";

    @Test
    public void testEnrich() throws Exception {


        HostFromJSONListAdapter hja = new HostFromJSONListAdapter(sample_known_hosts);

        JSONObject hfaJson = hja.enrich(new CacheKey("dummy", ip, null));

        Assert.assertNotNull(hfaJson);

        JSONParser jsonParser = new JSONParser();
        JSONObject finalEnriched = (JSONObject) jsonParser.parse(sampleMessage);

        Assert.assertEquals(finalEnriched, hfaJson);

    }

}

