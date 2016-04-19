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
package org.apache.metron.enrichment.bolt;

import com.google.common.collect.ImmutableSet;
import org.apache.metron.bolt.BaseEnrichmentBoltTest;
import org.apache.metron.domain.Enrichment;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;


public class EnrichmentSplitterBoltTest extends BaseEnrichmentBoltTest {

  @Test
  public void test() throws ParseException, IOException {
    final Enrichment geo = new Enrichment();
    geo.setType("geo");
    final Enrichment host = new Enrichment();
    host.setType("host");
    final Enrichment hbaseEnrichment = new Enrichment();
    hbaseEnrichment.setType("hbaseEnrichment");
    List<Enrichment> enrichments = new ArrayList<Enrichment>() {{
      add(geo);
      add(host);
      add(hbaseEnrichment);
    }};

    EnrichmentSplitterBolt enrichmentSplitterBolt = new EnrichmentSplitterBolt("zookeeperUrl").withEnrichments(enrichments);
    enrichmentSplitterBolt.setCuratorFramework(client);
    enrichmentSplitterBolt.setTreeCache(cache);
    enrichmentSplitterBolt.getConfigurations().updateSensorEnrichmentConfig(sensorType, new FileInputStream(sampleSensorEnrichmentConfigPath));
    enrichmentSplitterBolt.prepare(new HashMap<>(), topologyContext, outputCollector);

    String key = enrichmentSplitterBolt.getKey(tuple, sampleMessage);
    Assert.assertTrue(key != null && key.length() == 36);
    String someKey = "someKey";
    when(tuple.getStringByField("key")).thenReturn(someKey);
    key = enrichmentSplitterBolt.getKey(tuple, sampleMessage);
    Assert.assertEquals(someKey, key);
    when(tuple.getBinary(0)).thenReturn(sampleMessageString.getBytes());
    JSONObject generatedMessage = enrichmentSplitterBolt.generateMessage(tuple);
    removeTimingFields(generatedMessage);
    Assert.assertEquals(sampleMessage, generatedMessage);
    String messageFieldName = "messageFieldName";
    enrichmentSplitterBolt.withMessageFieldName(messageFieldName);
    when(tuple.getValueByField(messageFieldName)).thenReturn(sampleMessage);
    generatedMessage = enrichmentSplitterBolt.generateMessage(tuple);
    Assert.assertEquals(sampleMessage, generatedMessage);
    Set<String> actualStreamIds = enrichmentSplitterBolt.getStreamIds();
    Assert.assertEquals(streamIds, actualStreamIds);

    Map<String, JSONObject> actualSplitMessages = enrichmentSplitterBolt.splitMessage(sampleMessage);
    Assert.assertEquals(3, actualSplitMessages.size());
    Assert.assertEquals(geoMessage, actualSplitMessages.get("geo"));
    Assert.assertEquals(hostMessage, actualSplitMessages.get("host"));
    Assert.assertEquals(hbaseEnrichmentMessage, actualSplitMessages.get("hbaseEnrichment"));


  }

  public void removeTimingFields(JSONObject message) {
    ImmutableSet keys = ImmutableSet.copyOf(message.keySet());
    for(Object key: keys) {
      if (key.toString().contains("splitter.begin.ts")) {
        message.remove(key);
      }
    }
  }
}
