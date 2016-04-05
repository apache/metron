package org.apache.metron.enrichment;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.Constants;
import org.apache.metron.domain.SensorEnrichmentConfig;
import org.apache.metron.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnrichmentConfigTest {
  /**
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
  @Multiline
  public static String sourceConfigStr;

  /**
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
  @Multiline
  public static String threatIntelConfigStr;

  @Test
  public void testThreatIntel() throws Exception {

    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);


    EnrichmentConfig config = JSONUtils.INSTANCE.load(threatIntelConfigStr, EnrichmentConfig.class);
    final Map<String, SensorEnrichmentConfig> outputScs = new HashMap<>();
    EnrichmentConfig.SourceConfigHandler scHandler = new EnrichmentConfig.SourceConfigHandler() {
      @Override
      public SensorEnrichmentConfig readConfig(String sensor) throws Exception {
        if(sensor.equals("bro")) {
          return JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
        }
        else {
          throw new IllegalStateException("Tried to retrieve an unexpected sensor: " + sensor);
        }
      }

      @Override
      public void persistConfig(String sensor, SensorEnrichmentConfig config) throws Exception {
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

  /**
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
  @Multiline
  public static String enrichmentConfigStr;
  @Test
  public void testEnrichment() throws Exception {

    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);

    EnrichmentConfig config = JSONUtils.INSTANCE.load(enrichmentConfigStr, EnrichmentConfig.class);
    final Map<String, SensorEnrichmentConfig> outputScs = new HashMap<>();
    EnrichmentConfig.SourceConfigHandler scHandler = new EnrichmentConfig.SourceConfigHandler() {
      @Override
      public SensorEnrichmentConfig readConfig(String sensor) throws Exception {
        if(sensor.equals("bro")) {
          return JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
        }
        else {
          throw new IllegalStateException("Tried to retrieve an unexpected sensor: " + sensor);
        }
      }

      @Override
      public void persistConfig(String sensor, SensorEnrichmentConfig config) throws Exception {
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
