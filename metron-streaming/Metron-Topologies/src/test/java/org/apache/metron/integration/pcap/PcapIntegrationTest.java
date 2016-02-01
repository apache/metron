package org.apache.metron.integration.pcap;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.ElasticSearchComponent;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.mock.MockHBaseConnector;
import org.apache.metron.parsing.parsers.PcapParser;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.test.converters.HexStringConverter;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by cstella on 1/27/16.
 */
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
        Properties topologyProperties = new Properties() {{
            setProperty("input.path", "src/main/resources/");
            setProperty("es.port", "9300");
            setProperty("es.ip", "localhost");
            setProperty("es.clustername", "metron");
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
            setProperty("bolt.hbase.table.fields", "t:pcap");
            setProperty("bolt.hbase.table.key.tuple.field.name", "pcap_id");
            setProperty("bolt.hbase.table.timestamp.tuple.field.name", "timestamp");
            setProperty("bolt.hbase.enable.batching", "false");
            setProperty("bolt.hbase.write.buffer.size.in.bytes", "2000000");
            setProperty("bolt.hbase.durability", "SKIP_WAL");
            setProperty("bolt.hbase.partitioner.region.info.refresh.interval.mins","60");
            setProperty("hbase.connector.impl","org.apache.metron.integration.util.mock.MockHBaseConnector");
        }};
        FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
                                                                       .withTopologyLocation(new File(topologiesDir + "/pcap/local.yaml"))
                                                                       .withTopologyName("pcap")
                                                                       .withTopologyProperties(topologyProperties)
                                                                       .build();

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
                    if(docs.size() < expectedPcapIds.size() && MockHBaseConnector.getPuts().size() < expectedPcapIds.size()) {
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

        Assert.assertEquals(expectedPcapIds.size(), MockHBaseConnector.getPuts().size());
        UnitTestHelper.assertSetEqual("PCap IDs from Index"
                                     , new HashSet<String>(expectedPcapIds)
                                     , convertToSet(Iterables.transform(docs, DOC_TO_PCAP_ID))
                                     );
        UnitTestHelper.assertSetEqual("PCap IDs from HBase"
                                     , new HashSet<String>(expectedPcapIds)
                                     , convertToSet(Iterables.transform(MockHBaseConnector.getPuts(), RK_TO_PCAP_ID))
                                     );
        Iterable<PacketInfo> packetsFromHBase = Iterables.transform(MockHBaseConnector.getPuts(), PUT_TO_PCAP);
        Assert.assertEquals(expectedPcapIds.size(), Iterables.size(packetsFromHBase));
        MockHBaseConnector.clear();
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

    public static final Function<Put, PacketInfo> PUT_TO_PCAP = new Function<Put, PacketInfo>() {
        @Nullable
        public PacketInfo apply(@Nullable Put put) {
            try {
                return putToPcap(put);
            } catch (IOException e) {
                throw new RuntimeException("Unable to convert put to PCAP: " + put);
            }
        }
    };



    private static List<String> getExpectedPcap(File rawFile) throws IOException {
        List<String> ret = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(rawFile));
        for(String line = null; (line = br.readLine()) != null;) {
            byte[] pcapBytes = new HexStringConverter().convert(line);
            List<PacketInfo> list = PcapParser.parse(pcapBytes);
            for(PacketInfo pi : list) {
                String string_pcap = pi.getJsonIndexDoc();
        	    Object obj= JSONValue.parse(string_pcap);
        	    JSONObject header=(JSONObject)obj;
                ret.add((String)header.get("pcap_id"));
            }
        }
        return ret;
    }

    private static String getIndex() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.hh");
        Date d = new Date();
        return "pcap_index_" + sdf.format(d);
    }

    private static PacketInfo putToPcap(Put p) throws IOException {
        List<Cell> cells = p.get(Bytes.toBytes("t"), Bytes.toBytes("pcap"));
        Assert.assertEquals(1, cells.size());
        List<PacketInfo> l = PcapParser.parse(cells.get(0).getValueArray());
        Assert.assertEquals(1, l.size());
        return l.get(0);
    }

}
