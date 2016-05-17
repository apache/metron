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
package org.apache.metron.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.*;

import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.Constants;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.converter.EnrichmentHelper;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.mock.MockGeoAdapter;
import org.apache.metron.test.mock.MockHTable;
import org.apache.metron.enrichment.lookup.LookupKV;

import org.apache.metron.integration.utils.SampleUtil;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

public abstract class EnrichmentIntegrationTest extends BaseIntegrationTest {
  private static final String SRC_IP = "ip_src_addr";
  private static final String DST_IP = "ip_dst_addr";
  private static final String MALICIOUS_IP_TYPE = "malicious_ip";
  private static final String PLAYFUL_CLASSIFICATION_TYPE = "playful_classification";
  private static final Map<String, String> PLAYFUL_ENRICHMENT = new HashMap<String, String>() {{
    put("orientation", "north");
  }};
  private String fluxPath = "../metron-enrichment/src/main/flux/enrichment/test.yaml";
  protected String hdfsDir = "target/enrichmentIntegrationTest/hdfs";
  private String sampleParsedPath = TestConstants.SAMPLE_DATA_PARSED_PATH + "YafExampleParsed";
  private String sampleIndexedPath = TestConstants.SAMPLE_DATA_INDEXED_PATH + "YafIndexed";


  public static class Provider implements TableProvider, Serializable {
    MockHTable.Provider  provider = new MockHTable.Provider();
    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      return provider.getTable(config, tableName);
    }
  }

  public static void cleanHdfsDir(String hdfsDirStr) {
    File hdfsDir = new File(hdfsDirStr);
    Stack<File> fs = new Stack<>();
    if(hdfsDir.exists()) {
      fs.push(hdfsDir);
      while(!fs.empty()) {
        File f = fs.pop();
        if (f.isDirectory()) {
          for(File child : f.listFiles()) {
            fs.push(child);
          }
        }
        else {
          if (f.getName().startsWith("enrichment") || f.getName().endsWith(".json")) {
            f.delete();
          }
        }
      }
    }
  }

  public static List<byte[]> readSampleData(String samplePath) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(samplePath));
    List<byte[]> ret = new ArrayList<>();
    for (String line = null; (line = br.readLine()) != null; ) {
      ret.add(line.getBytes());
    }
    br.close();
    return ret;
  }

  public static List<Map<String, Object> > readDocsFromDisk(String hdfsDirStr) throws IOException {
    List<Map<String, Object>> ret = new ArrayList<>();
    File hdfsDir = new File(hdfsDirStr);
    Stack<File> fs = new Stack<>();
    if(hdfsDir.exists()) {
      fs.push(hdfsDir);
      while(!fs.empty()) {
        File f = fs.pop();
        if(f.isDirectory()) {
          for (File child : f.listFiles()) {
            fs.push(child);
          }
        }
        else {
          System.out.println("Processed " + f);
          if (f.getName().startsWith("enrichment") || f.getName().endsWith(".json")) {
            List<byte[]> data = readSampleData(f.getPath());
            Iterables.addAll(ret, Iterables.transform(data, new Function<byte[], Map<String, Object>>() {
              @Nullable
              @Override
              public Map<String, Object> apply(@Nullable byte[] bytes) {
                String s = new String(bytes);
                try {
                  return JSONUtils.INSTANCE.load(s, new TypeReference<Map<String, Object>>() {
                  });
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
            }));
          }
        }
      }
    }
    return ret;
  }

  @Test
  public void test() throws Exception {
    cleanHdfsDir(hdfsDir);
    final Configurations configurations = SampleUtil.getSampleConfigs();
    final String dateFormat = "yyyy.MM.dd.HH";
    final List<byte[]> inputMessages = readSampleData(sampleParsedPath);
    final String cf = "cf";
    final String trackerHBaseTableName = "tracker";
    final String threatIntelTableName = "threat_intel";
    final String enrichmentsTableName = "enrichments";
    final Properties topologyProperties = new Properties() {{
      setProperty("org.apache.metron.enrichment.host.known_hosts", "[{\"ip\":\"10.1.128.236\", \"local\":\"YES\", \"type\":\"webserver\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.1.128.237\", \"local\":\"UNKNOWN\", \"type\":\"unknown\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.60.10.254\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.0.2.15\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"}]");
      setProperty("hbase.provider.impl","" + Provider.class.getName());
      setProperty("threat.intel.tracker.table", trackerHBaseTableName);
      setProperty("threat.intel.tracker.cf", cf);
      setProperty("threat.intel.simple.hbase.table", threatIntelTableName);
      setProperty("threat.intel.simple.hbase.cf", cf);
      setProperty("enrichment.simple.hbase.table", enrichmentsTableName);
      setProperty("enrichment.simple.hbase.cf", cf);
      setProperty("es.clustername", "metron");
      setProperty("es.port", "9300");
      setProperty("es.ip", "localhost");
      setProperty("index.date.format", dateFormat);
      setProperty("index.hdfs.output", hdfsDir);
    }};
    setAdditionalProperties(topologyProperties);
    final KafkaWithZKComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }});

    //create MockHBaseTables
    final MockHTable trackerTable = (MockHTable)MockHTable.Provider.addToCache(trackerHBaseTableName, cf);
    final MockHTable threatIntelTable = (MockHTable)MockHTable.Provider.addToCache(threatIntelTableName, cf);
    EnrichmentHelper.INSTANCE.load(threatIntelTable, cf, new ArrayList<LookupKV<EnrichmentKey, EnrichmentValue>>(){{
      add(new LookupKV<>(new EnrichmentKey(MALICIOUS_IP_TYPE, "10.0.2.3"), new EnrichmentValue(new HashMap<String, String>())));
    }});
    final MockHTable enrichmentTable = (MockHTable)MockHTable.Provider.addToCache(enrichmentsTableName, cf);
    EnrichmentHelper.INSTANCE.load(enrichmentTable, cf, new ArrayList<LookupKV<EnrichmentKey, EnrichmentValue>>(){{
      add(new LookupKV<>(new EnrichmentKey(PLAYFUL_CLASSIFICATION_TYPE, "10.0.2.3")
                        , new EnrichmentValue(PLAYFUL_ENRICHMENT )
                        )
         );
    }});
    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(fluxPath))
            .withTopologyName("test")
            .withTopologyProperties(topologyProperties)
            .build();

    InMemoryComponent searchComponent = getSearchComponent(topologyProperties);

    UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaComponent)
            .withComponent("search", searchComponent)
            .withComponent("storm", fluxComponent)
            .withMillisecondsBetweenAttempts(10000)
            .withNumRetries(10)
            .build();
    runner.start();

    try {
      fluxComponent.submitTopology();

      kafkaComponent.writeMessages(Constants.ENRICHMENT_TOPIC, inputMessages);
      List<Map<String, Object>> docs = runner.process(getProcessor(inputMessages));
      Assert.assertEquals(inputMessages.size(), docs.size());
      List<Map<String, Object>> cleanedDocs = cleanDocs(docs);
      validateAll(cleanedDocs);


      List<Map<String, Object>> docsFromDisk = readDocsFromDisk(hdfsDir);
      Assert.assertEquals(docsFromDisk.size(), docs.size()) ;
      Assert.assertEquals(new File(hdfsDir).list().length, 1);
      Assert.assertEquals(new File(hdfsDir).list()[0], "yaf");
      validateAll(docsFromDisk);
    }
    finally {
      cleanHdfsDir(hdfsDir);
      runner.stop();
    }
  }

  public List<Map<String, Object>> cleanDocs(List<Map<String, Object>> docs) {
    List<Map<String, Object>> cleanedDocs = new ArrayList<>();
    for(Map<String, Object> doc: docs) {
      Map<String, Object> cleanedFields = new HashMap<>();
      for(String field: doc.keySet()) {
        cleanedFields.put(cleanField(field), doc.get(field));
      }
      cleanedDocs.add(cleanedFields);
    }
    return cleanedDocs;
  }

  public static void validateAll(List<Map<String, Object>> docs) {
    for (Map<String, Object> doc : docs) {
      baseValidation(doc);
      hostEnrichmentValidation(doc);
      geoEnrichmentValidation(doc);
      threatIntelValidation(doc);
      simpleEnrichmentValidation(doc);
    }
  }

  public static void baseValidation(Map<String, Object> jsonDoc) {
    assertEnrichmentsExists("threatintels.", setOf("hbaseThreatIntel"), jsonDoc.keySet());
    assertEnrichmentsExists("enrichments.", setOf("geo", "host", "hbaseEnrichment" ), jsonDoc.keySet());
    for(Map.Entry<String, Object> kv : jsonDoc.entrySet()) {
      //ensure no values are empty.
      Assert.assertTrue(kv.getValue().toString().length() > 0);
    }
    //ensure we always have a source ip and destination ip
    Assert.assertNotNull(jsonDoc.get(SRC_IP));
    Assert.assertNotNull(jsonDoc.get(DST_IP));
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
  private static void simpleEnrichmentValidation(Map<String, Object> indexedDoc) {
    if(indexedDoc.get(SRC_IP).equals("10.0.2.3")
            || indexedDoc.get(DST_IP).equals("10.0.2.3")
            ) {
      Assert.assertTrue(keyPatternExists("enrichments.hbaseEnrichment", indexedDoc));
      if(indexedDoc.get(SRC_IP).equals("10.0.2.3")) {
        Assert.assertEquals(indexedDoc.get("enrichments.hbaseEnrichment." + SRC_IP + "." + PLAYFUL_CLASSIFICATION_TYPE+ ".orientation")
                , PLAYFUL_ENRICHMENT.get("orientation")
        );
      }
      else if(indexedDoc.get(DST_IP).equals("10.0.2.3")) {
        Assert.assertEquals( indexedDoc.get("enrichments.hbaseEnrichment." + DST_IP + "." + PLAYFUL_CLASSIFICATION_TYPE + ".orientation")
                , PLAYFUL_ENRICHMENT.get("orientation")
        );
      }
    }

  }
  private static void threatIntelValidation(Map<String, Object> indexedDoc) {
    if(indexedDoc.get(SRC_IP).equals("10.0.2.3")
    || indexedDoc.get(DST_IP).equals("10.0.2.3")
            ) {
      //if we have any threat intel messages, we want to tag is_alert to true
      Assert.assertTrue(keyPatternExists("threatintels.", indexedDoc));
      Assert.assertTrue(indexedDoc.containsKey("threat.triage.level"));
      Assert.assertEquals(indexedDoc.get("is_alert"), "true");
      Assert.assertEquals((double)indexedDoc.get("threat.triage.level"), 10d, 1e-7);
    }
    else {
      //For YAF this is the case, but if we do snort later on, this will be invalid.
      Assert.assertNull(indexedDoc.get("is_alert"));
      Assert.assertFalse(keyPatternExists("threatintels.", indexedDoc));
    }
    //ip threat intels
    if(keyPatternExists("threatintels.hbaseThreatIntel.", indexedDoc)) {
      if(indexedDoc.get(SRC_IP).equals("10.0.2.3")) {
        Assert.assertEquals(indexedDoc.get("threatintels.hbaseThreatIntel." + SRC_IP + "." + MALICIOUS_IP_TYPE), "alert");
      }
      else if(indexedDoc.get(DST_IP).equals("10.0.2.3")) {
        Assert.assertEquals(indexedDoc.get("threatintels.hbaseThreatIntel." + DST_IP + "." + MALICIOUS_IP_TYPE), "alert");
      }
      else {
        Assert.fail("There was a threat intels that I did not expect: " + indexedDoc);
      }
    }

  }

  private static void geoEnrichmentValidation(Map<String, Object> indexedDoc) {
    //should have geo enrichment on every message due to mock geo adapter
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + DST_IP + ".location_point"), MockGeoAdapter.DEFAULT_LOCATION_POINT);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + SRC_IP +".location_point"), MockGeoAdapter.DEFAULT_LOCATION_POINT);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + DST_IP + ".longitude"), MockGeoAdapter.DEFAULT_LONGITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + SRC_IP + ".longitude"), MockGeoAdapter.DEFAULT_LONGITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + DST_IP + ".city"), MockGeoAdapter.DEFAULT_CITY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + SRC_IP + ".city"), MockGeoAdapter.DEFAULT_CITY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + DST_IP + ".latitude"), MockGeoAdapter.DEFAULT_LATITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + SRC_IP + ".latitude"), MockGeoAdapter.DEFAULT_LATITUDE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + DST_IP + ".country"), MockGeoAdapter.DEFAULT_COUNTRY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + SRC_IP + ".country"), MockGeoAdapter.DEFAULT_COUNTRY);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + DST_IP + ".dmaCode"), MockGeoAdapter.DEFAULT_DMACODE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + SRC_IP + ".dmaCode"), MockGeoAdapter.DEFAULT_DMACODE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + DST_IP + ".postalCode"), MockGeoAdapter.DEFAULT_POSTAL_CODE);
    Assert.assertEquals(indexedDoc.get("enrichments.geo." + SRC_IP + ".postalCode"), MockGeoAdapter.DEFAULT_POSTAL_CODE);
  }

  private static void hostEnrichmentValidation(Map<String, Object> indexedDoc) {
    boolean enriched = false;
    //important local printers
    {
      Set<String> ips = setOf("10.0.2.15", "10.60.10.254");
      if (ips.contains(indexedDoc.get(SRC_IP))) {
        //this is a local, important, printer
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.PRINTER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, SRC_IP))
        );
        enriched = true;
      }
      if (ips.contains(indexedDoc.get(DST_IP))) {
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.PRINTER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, DST_IP))
        );
        enriched = true;
      }
    }
    //important local webservers
    {
      Set<String> ips = setOf("10.1.128.236");
      if (ips.contains(indexedDoc.get(SRC_IP))) {
        //this is a local, important, printer
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.WEBSERVER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, SRC_IP))
        );
        enriched = true;
      }
      if (ips.contains(indexedDoc.get(DST_IP))) {
        Assert.assertTrue(Predicates.and(HostEnrichments.LOCAL_LOCATION
                ,HostEnrichments.IMPORTANT
                ,HostEnrichments.WEBSERVER_TYPE
                ).apply(new EvaluationPayload(indexedDoc, DST_IP))
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

  abstract public InMemoryComponent getSearchComponent(Properties topologyProperties) throws Exception;
  abstract public Processor<List<Map<String, Object>>> getProcessor(List<byte[]> inputMessages);
  abstract public void setAdditionalProperties(Properties topologyProperties);
  abstract public String cleanField(String field);

}
