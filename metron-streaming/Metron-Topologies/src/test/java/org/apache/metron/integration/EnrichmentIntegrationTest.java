/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.integration;

import com.google.common.base.*;
import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.Constants;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.integration.util.TestUtils;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.ElasticSearchComponent;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.util.mock.MockGeoAdapter;
import org.apache.metron.integration.util.mock.MockHTable;
import org.apache.metron.integration.util.threatintel.ThreatIntelHelper;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.utils.SourceConfigUtils;
import org.junit.Assert;
import org.junit.Test;
import org.apache.metron.utils.JSONUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class EnrichmentIntegrationTest {

  private String fluxPath = "src/main/resources/Metron_Configs/topologies/enrichment/test.yaml";
  private String indexDir = "target/elasticsearch";
  private String sampleParsedPath = "src/main/resources/SampleParsed/YafExampleParsed";
  private String sampleIndexedPath = "src/main/resources/SampleIndexed/YafIndexed";
  private Map<String, String> sourceConfigs = new HashMap<>();

  public static class Provider implements TableProvider, Serializable {
    MockHTable.Provider  provider = new MockHTable.Provider();
    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      return provider.getTable(config, tableName);
    }
  }


  @Test
  public void test() throws Exception {
    final String dateFormat = "yyyy.MM.dd.hh";
    final String index = "yaf_index_" + new SimpleDateFormat(dateFormat).format(new Date());
    String yafConfig = "{\n" +
            "  \"index\": \"yaf\",\n" +
            "  \"batchSize\": 5,\n" +
            "  \"enrichmentFieldMap\":\n" +
            "  {\n" +
            "    \"geo\": [\"sip\", \"dip\"],\n" +
            "    \"host\": [\"sip\", \"dip\"]\n" +
            "  },\n" +
            "  \"threatIntelFieldMap\":\n" +
            "  {\n" +
            "    \"ip\": [\"sip\", \"dip\"]\n" +
            "  }\n" +
            "}";
    sourceConfigs.put("yaf", yafConfig);
    final List<byte[]> inputMessages = TestUtils.readSampleData(sampleParsedPath);
    final String cf = "cf";
    final String trackerHBaseTable = "tracker";
    final String ipThreatIntelTable = "ip_threat_intel";
    final Properties topologyProperties = new Properties() {{
      setProperty("org.apache.metron.enrichment.host.known_hosts", "[{\"ip\":\"10.1.128.236\", \"local\":\"YES\", \"type\":\"webserver\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.1.128.237\", \"local\":\"UNKNOWN\", \"type\":\"unknown\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.60.10.254\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.0.2.15\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"}]");
      setProperty("hbase.provider.impl","" + Provider.class.getName());
      setProperty("threat.intel.tracker.table", trackerHBaseTable);
      setProperty("threat.intel.tracker.cf", cf);
      setProperty("threat.intel.ip.table", ipThreatIntelTable);
      setProperty("threat.intel.ip.cf", cf);
      setProperty("es.clustername", "metron");
      setProperty("es.port", "9300");
      setProperty("es.ip", "localhost");
      setProperty("index.date.format", dateFormat);
    }};
    final KafkaWithZKComponent kafkaComponent = new KafkaWithZKComponent().withTopics(new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }})
            .withPostStartCallback(new Function<KafkaWithZKComponent, Void>() {
              @Nullable
              @Override
              public Void apply(@Nullable KafkaWithZKComponent kafkaWithZKComponent) {
                topologyProperties.setProperty("kafka.zk", kafkaWithZKComponent.getZookeeperConnect());
                try {
                  for(String sourceType: sourceConfigs.keySet()) {
                    SourceConfigUtils.writeToZookeeper(sourceType, sourceConfigs.get(sourceType).getBytes(), kafkaWithZKComponent.getZookeeperConnect());
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            });

    ElasticSearchComponent esComponent = new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .build();

    //create MockHBaseTables
    final MockHTable trackerTable = (MockHTable)MockHTable.Provider.addToCache(trackerHBaseTable, cf);
    final MockHTable ipTable = (MockHTable)MockHTable.Provider.addToCache(ipThreatIntelTable, cf);
    ThreatIntelHelper.INSTANCE.load(ipTable, cf, new ArrayList<LookupKV<ThreatIntelKey, ThreatIntelValue>>(){{
      add(new LookupKV<>(new ThreatIntelKey("10.0.2.3"), new ThreatIntelValue(new HashMap<String, String>())));
    }});

    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(fluxPath))
            .withTopologyName("test")
            .withTopologyProperties(topologyProperties)
            .build();

    UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaComponent)
            .withComponent("elasticsearch", esComponent)
            .withComponent("storm", fluxComponent)
            .withMillisecondsBetweenAttempts(10000)
            .withNumRetries(30)
            .withMaxTimeMS(300000)
            .build();
    runner.start();
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(Constants.ENRICHMENT_TOPIC, inputMessages);
    List<Map<String, Object>> docs =
            runner.process(new Processor<List<Map<String, Object>>> () {
              List<Map<String, Object>> docs = null;
              public ReadinessState process(ComponentRunner runner){
                ElasticSearchComponent elasticSearchComponent = runner.getComponent("elasticsearch", ElasticSearchComponent.class);
                if(elasticSearchComponent.hasIndex(index)) {
                  try {
                    docs = elasticSearchComponent.getAllIndexedDocs(index, "yaf_doc");
                  } catch (IOException e) {
                    throw new IllegalStateException("Unable to retrieve indexed documents.", e);
                  }
                  if(docs.size() < inputMessages.size()) {
                    return ReadinessState.NOT_READY;
                  }
                  else {
                    return ReadinessState.READY;
                  }
                }
                else {
                  return ReadinessState.NOT_READY;
                }
              }

              public List<Map<String, Object>> getResult() {
                return docs;
              }
            });

    List<byte[]> sampleIndexedMessages = TestUtils.readSampleData(sampleIndexedPath);
    Assert.assertEquals(sampleIndexedMessages.size(), docs.size());

    for (Map<String, Object> doc : docs) {
      baseValidation(doc);

      hostEnrichmentValidation(doc);
      geoEnrichmentValidation(doc);
      threatIntelValidation(doc);

    }
    runner.stop();
  }

  public static void baseValidation(Map<String, Object> jsonDoc) {
    assertEnrichmentsExists("threatintels.", setOf("ip"), jsonDoc.keySet());
    assertEnrichmentsExists("enrichments.", setOf("geo", "host"), jsonDoc.keySet());
    for(Map.Entry<String, Object> kv : jsonDoc.entrySet()) {
      //ensure no values are empty.
      Assert.assertTrue(kv.getValue().toString().length() > 0);
    }
    //ensure we always have a source ip and destination ip
    Assert.assertNotNull(jsonDoc.get("sip"));
    Assert.assertNotNull(jsonDoc.get("dip"));
  }

  private static class EvaluationPayload {
    Map<String, Object> indexedDoc;
    String key;
    public EvaluationPayload(Map<String, Object> indexedDoc, String key) {
      this.indexedDoc = indexedDoc;
      this.key = key;
    }
  }

  private static enum HostEnrichments implements Predicate<EvaluationPayload>{
    LOCAL_LOCATION(new Predicate<EvaluationPayload>() {

      @Override
      public boolean apply(@Nullable EvaluationPayload evaluationPayload) {
        return evaluationPayload.indexedDoc.get("enrichments.host." + evaluationPayload.key + ".known_info.local").equals("YES");
      }
    })
    ,UNKNOWN_LOCATION(new Predicate<EvaluationPayload>() {

      @Override
      public boolean apply(@Nullable EvaluationPayload evaluationPayload) {
        return evaluationPayload.indexedDoc.get("enrichments.host." + evaluationPayload.key + ".known_info.local").equals("UNKNOWN");
      }
    })
    ,IMPORTANT(new Predicate<EvaluationPayload>() {
      @Override
      public boolean apply(@Nullable EvaluationPayload evaluationPayload) {
        return evaluationPayload.indexedDoc.get("enrichments.host." + evaluationPayload.key + ".known_info.asset_value").equals("important");
      }
    })
    ,PRINTER_TYPE(new Predicate<EvaluationPayload>() {
      @Override
      public boolean apply(@Nullable EvaluationPayload evaluationPayload) {
        return evaluationPayload.indexedDoc.get("enrichments.host." + evaluationPayload.key + ".known_info.type").equals("printer");
      }
    })
    ,WEBSERVER_TYPE(new Predicate<EvaluationPayload>() {
      @Override
      public boolean apply(@Nullable EvaluationPayload evaluationPayload) {
        return evaluationPayload.indexedDoc.get("enrichments.host." + evaluationPayload.key + ".known_info.type").equals("webserver");
      }
    })
    ,UNKNOWN_TYPE(new Predicate<EvaluationPayload>() {
      @Override
      public boolean apply(@Nullable EvaluationPayload evaluationPayload) {
        return evaluationPayload.indexedDoc.get("enrichments.host." + evaluationPayload.key + ".known_info.type").equals("unknown");
      }
    })
    ;

    Predicate<EvaluationPayload> _predicate;
    HostEnrichments(Predicate<EvaluationPayload> predicate) {
      this._predicate = predicate;
    }

    public boolean apply(EvaluationPayload payload) {
      return _predicate.apply(payload);
    }

  }

  private static void assertEnrichmentsExists(String topLevel, Set<String> expectedEnrichments, Set<String> keys) {
    for(String key : keys) {
      if(key.startsWith(topLevel)) {
        String secondLevel = Iterables.get(Splitter.on(".").split(key), 1);
        String message = "Found an enrichment/threat intel (" + secondLevel + ") that I didn't expect (expected enrichments :"
                       + Joiner.on(",").join(expectedEnrichments) + "), but it was not there.  If you've created a new"
                       + " enrichment, then please add a validation method to this unit test.  Otherwise, it's a solid error"
                       + " and should be investigated.";
        Assert.assertTrue( message, expectedEnrichments.contains(secondLevel));
      }
    }
  }
  private static void threatIntelValidation(Map<String, Object> indexedDoc) {
    if(keyPatternExists("threatintels.", indexedDoc)) {
      //if we have any threat intel messages, we want to tag is_alert to true
      Assert.assertEquals(indexedDoc.get("is_alert"), "true");
    }
    else {
      //For YAF this is the case, but if we do snort later on, this will be invalid.
      Assert.assertNull(indexedDoc.get("is_alert"));
    }
    //ip threat intels
    if(keyPatternExists("threatintels.ip.", indexedDoc)) {
      if(indexedDoc.get("sip").equals("10.0.2.3")) {
        Assert.assertEquals(indexedDoc.get("threatintels.ip.sip.ip_threat_intel"), "alert");
      }
      else if(indexedDoc.get("dip").equals("10.0.2.3")) {
        Assert.assertEquals(indexedDoc.get("threatintels.ip.dip.ip_threat_intel"), "alert");
      }
      else {
        Assert.fail("There was a threat intels that I did not expect.");
      }
    }

  }

  private static void geoEnrichmentValidation(Map<String, Object> indexedDoc) {
    //should have geo enrichment on every message due to mock geo adapter
    Assert.assertEquals(indexedDoc.get("enrichments.geo.dip.location_point"), MockGeoAdapter.DEFAULT_LOCATION_POINT);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.sip.location_point"), MockGeoAdapter.DEFAULT_LOCATION_POINT);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.dip.longitude"), MockGeoAdapter.DEFAULT_LONGITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.sip.longitude"), MockGeoAdapter.DEFAULT_LONGITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.dip.city"), MockGeoAdapter.DEFAULT_CITY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.sip.city"), MockGeoAdapter.DEFAULT_CITY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.dip.latitude"), MockGeoAdapter.DEFAULT_LATITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.sip.latitude"), MockGeoAdapter.DEFAULT_LATITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.dip.country"), MockGeoAdapter.DEFAULT_COUNTRY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.sip.country"), MockGeoAdapter.DEFAULT_COUNTRY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.dip.dmaCode"), MockGeoAdapter.DEFAULT_DMACODE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.sip.dmaCode"), MockGeoAdapter.DEFAULT_DMACODE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.dip.postalCode"), MockGeoAdapter.DEFAULT_POSTAL_CODE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo.sip.postalCode"), MockGeoAdapter.DEFAULT_POSTAL_CODE);
  }

  private static void hostEnrichmentValidation(Map<String, Object> indexedDoc) {
    boolean enriched = false;
    //important local printers
    {
      Set<String> ips = setOf("10.0.2.15", "10.60.10.254");
      if (ips.contains(indexedDoc.get("sip"))) {
        //this is a local, important, printer
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.PRINTER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, "sip"))
        );
        enriched = true;
      }
      if (ips.contains(indexedDoc.get("dip"))) {
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.PRINTER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, "dip"))
        );
        enriched = true;
      }
    }
    //important local webservers
    {
      Set<String> ips = setOf("10.1.128.236");
      if (ips.contains(indexedDoc.get("sip"))) {
        //this is a local, important, printer
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.WEBSERVER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, "sip"))
        );
        enriched = true;
      }
      if (ips.contains(indexedDoc.get("dip"))) {
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.WEBSERVER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, "dip"))
        );
        enriched = true;
      }
    }
    if(!enriched) {
      Assert.assertFalse(keyPatternExists("enrichments.host", indexedDoc));
    }
  }


  private static boolean keyPatternExists(String pattern, Map<String, Object> indexedObj) {
    for(String k : indexedObj.keySet()) {
      if(k.startsWith(pattern)) {
        return true;
      }
    }
    return false;
  }
  private static Set<String> setOf(String... items) {
    Set<String> ret = new HashSet<>();
    for(String item : items) {
      ret.add(item);
    }
    return ret;
  }

}
