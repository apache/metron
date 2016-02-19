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
package org.apache.metron.integration.pcap;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.ElasticSearchComponent;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.mock.MockHTable;
import org.apache.metron.integration.util.threatintel.ThreatIntelHelper;
import org.apache.metron.parsing.parsers.PcapParser;
import org.apache.metron.test.converters.HexStringConverter;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PcapIntegrationTest {

    private String topologiesDir = "src/main/resources/Metron_Configs/topologies";
    private String targetDir = "target";


    @Test
    public void testTopology() throws Exception {
        if(!new File(topologiesDir).exists()) {
            topologiesDir = UnitTestHelper.findDir("topologies");
        }
        if(!new File(targetDir).exists()) {
            targetDir = UnitTestHelper.findDir("target");
        }
        Assert.assertNotNull(topologiesDir);
        Assert.assertNotNull(targetDir);
        final List<String> expectedPcapIds= getExpectedPcap(new File(topologiesDir + "/../../SampleInput/PCAPExampleOutput"));
        Assert.assertTrue("Expected non-zero number of PCAP Ids from the sample data", expectedPcapIds.size() > 0);
        System.out.println("Using topologies directory: " + topologiesDir);

        ElasticSearchComponent esComponent = new ElasticSearchComponent.Builder()
                                                                       .withHttpPort(9211)
                                                                       .withIndexDir(new File(targetDir + "/elasticsearch"))
                                                                       .build();
        final String cf = "cf";
        final String trackerHBaseTable = "tracker";
        final String ipThreatIntelTable = "ip_threat_intel";
        Properties topologyProperties = new Properties() {{
            setProperty("input.path", "src/main/resources/");
            setProperty("es.port", "9300");
            setProperty("es.ip", "localhost");
            setProperty("es.clustername", "metron");
            setProperty("mysql.ip", "node1");
            setProperty("mysql.port", "3306");
            setProperty("mysql.username", "root");
            setProperty("mysql.password", "P@ssw0rd");
            setProperty("pcap.binary.converter", "FROM_HEX_STRING");
            setProperty("testing.repeating", "false");
            setProperty("org.apache.metron.metrics.reporter.graphite", "false");
            setProperty("org.apache.metron.metrics.reporter.console", "false");
            setProperty("org.apache.metron.metrics.reporter.jmx", "false");
            setProperty("org.apache.metron.metrics.TelemetryParserBolt.acks","true");
            setProperty("org.apache.metron.metrics.TelemetryParserBolt.emits", "true");
            setProperty("org.apache.metron.metrics.TelemetryParserBolt.fails","true");
            setProperty("org.apache.metron.metrics.GenericEnrichmentBolt.acks","true");
            setProperty("org.apache.metron.metrics.GenericEnrichmentBolt.emits","true");
            setProperty("org.apache.metron.metrics.GenericEnrichmentBolt.fails","true");
            setProperty("org.apache.metron.metrics.TelemetryIndexingBolt.acks", "true");
            setProperty("org.apache.metron.metrics.TelemetryIndexingBolt.emits","true");
            setProperty("org.apache.metron.metrics.TelemetryIndexingBolt.fails","true");
            setProperty("kafka.zk", "localhost:2000,localhost:2000");
            setProperty("bolt.hbase.table.name", "pcap_test");
            setProperty("bolt.hbase.table.fields", "t:value");
            setProperty("bolt.hbase.table.key.tuple.field.name", "key");
            setProperty("bolt.hbase.table.timestamp.tuple.field.name", "timestamp");
            setProperty("bolt.hbase.enable.batching", "false");
            setProperty("bolt.hbase.write.buffer.size.in.bytes", "2000000");
            setProperty("bolt.hbase.durability", "SKIP_WAL");
            setProperty("bolt.hbase.partitioner.region.info.refresh.interval.mins","60");
            setProperty("hbase.provider.impl","" + MockHTable.Provider.class.getName());
            setProperty("threat.intel.tracker.table", trackerHBaseTable);
            setProperty("threat.intel.tracker.cf", cf);
            setProperty("threat.intel.ip.table", ipThreatIntelTable);
            setProperty("threat.intel.ip.cf", cf);
            setProperty("org.apache.metron.enrichment.host.known_hosts", "[{\"ip\":\"10.1.128.236\", \"local\":\"YES\", \"type\":\"webserver\", \"asset_value\" : \"important\"}," +
                    "{\"ip\":\"10.1.128.237\", \"local\":\"UNKNOWN\", \"type\":\"unknown\", \"asset_value\" : \"important\"}," +
                    "{\"ip\":\"10.60.10.254\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"}," +
                    "{\"ip\":\"10.0.2.15\", \"local\":\"YES\", " +
                    "\"type\":\"printer\", \"asset_value\" : \"important\"}]");
        }};
        //create MockHBaseTables
        final MockHTable trackerTable = (MockHTable)MockHTable.Provider.addToCache(trackerHBaseTable, cf);
        final MockHTable ipTable = (MockHTable)MockHTable.Provider.addToCache(ipThreatIntelTable, cf);
        ThreatIntelHelper.INSTANCE.load(ipTable, cf, new ArrayList<ThreatIntelResults>(){{
            add(new ThreatIntelResults(new ThreatIntelKey("10.0.2.3"), new HashMap<String, String>()));
        }}, 0L);
        final MockHTable pcapTable = (MockHTable) MockHTable.Provider.addToCache("pcap_test", "t");
        FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
                                                                       .withTopologyLocation(new File(topologiesDir + "/pcap/local.yaml"))
                                                                       .withTopologyName("pcap")
                                                                       .withTopologyProperties(topologyProperties)
                                                                       .build();
        //UnitTestHelper.verboseLogging();
        ComponentRunner runner = new ComponentRunner.Builder()
                                                    .withComponent("elasticsearch", esComponent)
                                                    .withComponent("storm", fluxComponent)
                                                    .build();

        final String index = getIndex();
        System.out.println("Index of the run: " + index);
        runner.start();
        fluxComponent.submitTopology();
        List<Map<String, Object>> docs =
        runner.process(new Processor<List<Map<String, Object>>> () {
            List<Map<String, Object>> docs = null;
            public ReadinessState process(ComponentRunner runner){
                ElasticSearchComponent elasticSearchComponent = runner.getComponent("elasticsearch", ElasticSearchComponent.class);
                if(elasticSearchComponent.hasIndex(index)) {
                    try {
                        docs = elasticSearchComponent.getAllIndexedDocs(index);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to retrieve indexed documents.", e);
                    }
                    if(docs.size() < expectedPcapIds.size() && pcapTable.getPutLog().size() < expectedPcapIds.size()) {
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

        Assert.assertEquals(expectedPcapIds.size(), pcapTable.getPutLog().size());
        UnitTestHelper.assertSetEqual("PCap IDs from Index"
                                     , new HashSet<>(expectedPcapIds)
                                     , convertToSet(Iterables.transform(docs, DOC_TO_PCAP_ID))
                                     );
        UnitTestHelper.assertSetEqual("PCap IDs from HBase"
                                     , new HashSet<>(expectedPcapIds)
                                     , convertToSet(Iterables.transform(pcapTable.getPutLog(), RK_TO_PCAP_ID))
                                     );
        Iterable<JSONObject> packetsFromHBase = Iterables.transform(pcapTable.getPutLog(), PUT_TO_PCAP);
        Assert.assertEquals(expectedPcapIds.size(), Iterables.size(packetsFromHBase));

        List<Map<String, Object>> allDocs= runner.getComponent("elasticsearch", ElasticSearchComponent.class).getAllIndexedDocs(index, null);
        boolean hasThreat = false;
        for(Map<String, Object> d : allDocs) {
            Map<String, Object> message = (Map<String, Object>) d.get("message");
            Set<String> ips = new HashSet<>(Arrays.asList((String)message.get("ip_dst_addr"), (String)message.get("ip_src_addr")));
            if(ips.contains("10.0.2.3")) {
                hasThreat = true;
                Map<String, Object> alerts = (Map<String, Object>) ((Map<String, Object>) d.get("alerts")).get("ip");
                Assert.assertTrue(  ((Map<String,Object>)alerts.get("ip_dst_addr")).size() > 0
                                 || ((Map<String,Object>)alerts.get("ip_src_addr")).size() > 0
                                 );
            }
        }
        Assert.assertTrue(hasThreat);
        MockHTable.Provider.clear();
        runner.stop();
    }

    public static Set<String> convertToSet(Iterable<String> strings) {
        Set<String> ret = new HashSet<String>();
        Iterables.addAll(ret, strings);
        return ret;
    }
    public static final Function<Put, String> RK_TO_PCAP_ID = new Function<Put, String>() {
        @Nullable
        public String apply(@Nullable Put put) {
            String rk =new String(put.getRow());
            return Joiner.on("-").join(Iterables.limit(Splitter.on('-').split(rk), 5));
        }
    };

    public static final Function<Map<String, Object>, String> DOC_TO_PCAP_ID = new Function<Map<String, Object>, String>() {

        @Nullable
        public String apply(@Nullable Map<String, Object> doc) {
            return (String)doc.get("pcap_id");
        }
    };

    public static final Function<Put, JSONObject> PUT_TO_PCAP = new
            Function<Put, JSONObject>() {
        @Nullable
        public JSONObject apply(@Nullable Put put) {
            try {
                return putToPcap(put);
            } catch (IOException e) {
                throw new RuntimeException("Unable to convert put to PCAP: " + put);
            }
        }
    };



    private static List<String> getExpectedPcap(File rawFile) throws IOException {
        List<String> ret = new ArrayList<String>();
        PcapParser parser = new PcapParser();
        parser.withTsPrecision("MICRO");
        parser.init();
        BufferedReader br = new BufferedReader(new FileReader(rawFile));
        for(String line = null; (line = br.readLine()) != null;) {
            byte[] pcapBytes = new HexStringConverter().convert(line);
            List<JSONObject> list = parser.parse(pcapBytes);
            for(JSONObject message : list) {
                ret.add((String) message.get("pcap_id"));
            }
        }
        return ret;
    }

    private static String getIndex() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.hh");
        Date d = new Date();
        return "pcap_index_" + sdf.format(d);
    }

    private static JSONObject putToPcap(Put p) throws IOException {
        PcapParser parser = new PcapParser();
        parser.init();
        List<Cell> cells = p.get(Bytes.toBytes("t"), Bytes.toBytes("value"));
        Assert.assertEquals(1, cells.size());
        List<JSONObject> messages = parser.parse(cells.get(0).getValueArray());
        Assert.assertEquals(1, messages.size());
        return messages.get(0);
    }

}
