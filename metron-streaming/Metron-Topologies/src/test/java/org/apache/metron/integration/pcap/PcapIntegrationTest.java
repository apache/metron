package org.apache.metron.integration.pcap;

import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.ElasticSearchComponent;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.parsing.parsers.PcapParser;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.test.converters.HexStringConverter;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;

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
        final Set<String> expectedPcapIds= getExpectedPcap(new File(topologiesDir + "/../../SampleInput/PCAPExampleOutput"));
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
        runner.start();
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
                    if(docs.size() < expectedPcapIds.size()) {
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
        checkDocuments(expectedPcapIds, docs);
        runner.stop();
    }

    private static void checkDocuments(Set<String> expectedPcapIds, List<Map<String, Object>> documents) {

        boolean mismatch = false;
        Set<String> indexedPcapIds = new HashSet<String>();
        for(Map<String, Object> doc : documents) {
            String indexedId = (String)doc.get("pcap_id");
            indexedPcapIds.add(indexedId);
            if(!expectedPcapIds.contains(indexedId)) {
                mismatch = true;
                System.out.println("Indexed PCAP ID that I did not expect: " + indexedId);
            }
        }
        for(String expectedId : expectedPcapIds) {
            if(!indexedPcapIds.contains(expectedId)) {
                mismatch = true;
                System.out.println("Expected PCAP ID that I did not index: " + expectedId);
            }
        }
        Assert.assertFalse(mismatch);
    }

    private static Set<String> getExpectedPcap(File rawFile) throws IOException {
        Set<String> ret = new HashSet<String>();
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

}
