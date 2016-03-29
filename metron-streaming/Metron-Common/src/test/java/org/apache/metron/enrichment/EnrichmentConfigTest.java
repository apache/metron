package org.apache.metron.enrichment;

import org.apache.metron.Constants;
import org.apache.metron.domain.SourceConfig;
import org.apache.metron.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnrichmentConfigTest {
  @Test
  public void testThreatIntel() throws Exception {
    /*
    {
      "index": "bro",
      "batchSize": 5,
      "enrichmentFieldMap": {
        "geo": ["ip_dst_addr", "ip_src_addr"],
        "host": ["host"]
                            },
      "threatIntelFieldMap": {
        "hbaseThreatIntel": ["ip_dst_addr", "ip_src_addr"]
                             },
      "fieldToThreatIntelTypeMap": {
        "ip_dst_addr" : [ "malicious_ip" ]
       ,"ip_src_addr" : [ "malicious_ip" ]
                                   }
    }
     */
    final String sourceConfigStr = "    {\n" +
            "      \"index\": \"bro\",\n" +
            "      \"batchSize\": 5,\n" +
            "      \"enrichmentFieldMap\": {\n" +
            "        \"geo\": [\"ip_dst_addr\", \"ip_src_addr\"],\n" +
            "        \"host\": [\"host\"]\n" +
            "                            },\n" +
            "      \"threatIntelFieldMap\": {\n" +
            "        \"hbaseThreatIntel\": [\"ip_dst_addr\", \"ip_src_addr\"]\n" +
            "                             },\n" +
            "      \"fieldToThreatIntelTypeMap\": {\n" +
            "        \"ip_dst_addr\" : [ \"malicious_ip\" ]\n" +
            "       ,\"ip_src_addr\" : [ \"malicious_ip\" ]\n" +
            "                                   }\n" +
            "    }";
    SourceConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SourceConfig.class);
    /*
{
  "zkQuorum" : "localhost:2181"
 ,"sensorToFieldList" : {
  "bro" : {
           "type" : "THREAT_INTEL"
          ,"fieldToEnrichmentTypes" : {
            "ip_src_addr" : [ "playful" ]
           ,"ip_dst_addr" : [ "playful" ]
                                      }
          }
                        }
}
     */
    String enrichmentConfigStr = "{\n" +
            "  \"zkQuorum\" : \"localhost:2181\"\n" +
            " ,\"sensorToFieldList\" : {\n" +
            "  \"bro\" : {\n" +
            "           \"type\" : \"THREAT_INTEL\"\n" +
            "          ,\"fieldToEnrichmentTypes\" : {\n" +
            "            \"ip_src_addr\" : [ \"playful\" ]\n" +
            "           ,\"ip_dst_addr\" : [ \"playful\" ]\n" +
            "                                      }\n" +
            "          }\n" +
            "                        }\n" +
            "}";
    EnrichmentConfig config = JSONUtils.INSTANCE.load(enrichmentConfigStr, EnrichmentConfig.class);
    final Map<String, SourceConfig> outputScs = new HashMap<>();
    EnrichmentConfig.SourceConfigHandler scHandler = new EnrichmentConfig.SourceConfigHandler() {
      @Override
      public SourceConfig readConfig(String sensor) throws Exception {
        if(sensor.equals("bro")) {
          return JSONUtils.INSTANCE.load(sourceConfigStr, SourceConfig.class);
        }
        else {
          throw new IllegalStateException("Tried to retrieve an unexpected sensor: " + sensor);
        }
      }

      @Override
      public void persistConfig(String sensor, SourceConfig config) throws Exception {
        outputScs.put(sensor, config);
      }
    };
    EnrichmentConfig.updateSensorConfigs(scHandler, config.getSensorToFieldList());
    Assert.assertNotNull(outputScs.get("bro"));
    Assert.assertNotSame(outputScs.get("bro"), broSc);
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getThreatIntelFieldMap().get(Constants.SIMPLE_HBASE_THREAT_INTEL).size()
                       , 2
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getThreatIntelFieldMap()
                                  .get(Constants.SIMPLE_HBASE_THREAT_INTEL)
                                  .contains("ip_src_addr")
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getThreatIntelFieldMap()
                                  .get(Constants.SIMPLE_HBASE_THREAT_INTEL)
                                  .contains("ip_dst_addr")
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToThreatIntelTypeMap().keySet().size()
                       , 2
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToThreatIntelTypeMap().get("ip_src_addr").size()
                       , 2
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToThreatIntelTypeMap().get("ip_src_addr").contains("playful")
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToThreatIntelTypeMap().get("ip_src_addr").contains("malicious_ip")
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToThreatIntelTypeMap().get("ip_dst_addr").size()
                       , 2
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToThreatIntelTypeMap().get("ip_dst_addr").contains("playful")
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToThreatIntelTypeMap().get("ip_dst_addr").contains("malicious_ip")
                       );
  }
  @Test
  public void testEnrichment() throws Exception {
    /*
    {
      "index": "bro",
      "batchSize": 5,
      "enrichmentFieldMap": {
        "geo": ["ip_dst_addr", "ip_src_addr"],
        "host": ["host"]
                            },
      "threatIntelFieldMap": {
        "hbaseThreatIntel": ["ip_dst_addr", "ip_src_addr"]
                             },
      "fieldToThreatIntelTypeMap": {
        "ip_dst_addr" : [ "malicious_ip" ]
       ,"ip_src_addr" : [ "malicious_ip" ]
                                   }
    }
     */
    final String sourceConfigStr = "    {\n" +
            "      \"index\": \"bro\",\n" +
            "      \"batchSize\": 5,\n" +
            "      \"enrichmentFieldMap\": {\n" +
            "        \"geo\": [\"ip_dst_addr\", \"ip_src_addr\"],\n" +
            "        \"host\": [\"host\"]\n" +
            "                            },\n" +
            "      \"threatIntelFieldMap\": {\n" +
            "        \"hbaseThreatIntel\": [\"ip_dst_addr\", \"ip_src_addr\"]\n" +
            "                             },\n" +
            "      \"fieldToThreatIntelTypeMap\": {\n" +
            "        \"ip_dst_addr\" : [ \"malicious_ip\" ]\n" +
            "       ,\"ip_src_addr\" : [ \"malicious_ip\" ]\n" +
            "                                   }\n" +
            "    }";
    SourceConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SourceConfig.class);
    /*
{
  "zkQuorum" : "localhost:2181"
 ,"sensorToFieldList" : {
  "bro" : {
           "type" : "ENRICHMENT"
          ,"fieldToEnrichmentTypes" : {
            "ip_src_addr" : [ "playful" ]
           ,"ip_dst_addr" : [ "playful" ]
                                      }
          }
                        }
}
     */
    String enrichmentConfigStr = "{\n" +
            "  \"zkQuorum\" : \"localhost:2181\"\n" +
            " ,\"sensorToFieldList\" : {\n" +
            "  \"bro\" : {\n" +
            "           \"type\" : \"ENRICHMENT\"\n" +
            "          ,\"fieldToEnrichmentTypes\" : {\n" +
            "            \"ip_src_addr\" : [ \"playful\" ]\n" +
            "           ,\"ip_dst_addr\" : [ \"playful\" ]\n" +
            "                                      }\n" +
            "          }\n" +
            "                        }\n" +
            "}";
    EnrichmentConfig config = JSONUtils.INSTANCE.load(enrichmentConfigStr, EnrichmentConfig.class);
    final Map<String, SourceConfig> outputScs = new HashMap<>();
    EnrichmentConfig.SourceConfigHandler scHandler = new EnrichmentConfig.SourceConfigHandler() {
      @Override
      public SourceConfig readConfig(String sensor) throws Exception {
        if(sensor.equals("bro")) {
          return JSONUtils.INSTANCE.load(sourceConfigStr, SourceConfig.class);
        }
        else {
          throw new IllegalStateException("Tried to retrieve an unexpected sensor: " + sensor);
        }
      }

      @Override
      public void persistConfig(String sensor, SourceConfig config) throws Exception {
        outputScs.put(sensor, config);
      }
    };
    EnrichmentConfig.updateSensorConfigs(scHandler, config.getSensorToFieldList());
    Assert.assertNotNull(outputScs.get("bro"));
    Assert.assertNotSame(outputScs.get("bro"), broSc);
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getEnrichmentFieldMap().get(Constants.SIMPLE_HBASE_ENRICHMENT).size()
                       , 2
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getEnrichmentFieldMap()
                                  .get(Constants.SIMPLE_HBASE_ENRICHMENT)
                                  .contains("ip_src_addr")
                       );
    Assert.assertTrue( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getEnrichmentFieldMap()
                                  .get(Constants.SIMPLE_HBASE_ENRICHMENT)
                                  .contains("ip_dst_addr")
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToEnrichmentTypeMap().keySet().size()
                       , 2
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToEnrichmentTypeMap().get("ip_src_addr").size()
                       , 1
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToEnrichmentTypeMap().get("ip_src_addr").get(0)
                       , "playful"
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToEnrichmentTypeMap().get("ip_dst_addr").size()
                       , 1
                       );
    Assert.assertEquals( outputScs.get("bro").toJSON()
                       , outputScs.get("bro").getFieldToEnrichmentTypeMap().get("ip_dst_addr").get(0)
                       , "playful"
                       );
  }
}
