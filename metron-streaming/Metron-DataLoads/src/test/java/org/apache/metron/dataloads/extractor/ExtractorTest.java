package org.apache.metron.dataloads.extractor;

import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cstella on 2/3/16.
 */
public class ExtractorTest {
    public static class DummyExtractor implements Extractor
    {

        @Override
        public ThreatIntelResults extract(String line) throws IOException {
            ThreatIntelKey key = new ThreatIntelKey();
            key.indicator = "dummy";
            Map<String, String> value = new HashMap<>();
            value.put("indicator", "dummy");
            return new ThreatIntelResults(key, value);
        }

        @Override
        public void initialize(Map<String, Object> config) {

        }
    }
    @Test
    public void testDummyExtractor() throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
        Extractor extractor = Extractors.create(DummyExtractor.class.getName());
        ThreatIntelResults results = extractor.extract(null);
        Assert.assertEquals("dummy", results.getKey().indicator);
        Assert.assertEquals("dummy", results.getValue().get("indicator"));
    }

    @Test
    public void testExtractionLoading() throws Exception {
        /**
         config:
         {
            "config" : {}
            ,"extractor" : "org.apache.metron.dataloads.extractor.ExtractorTest$DummyExtractor"
         }
         */
        String config = "{\n" +
                "            \"config\" : {}\n" +
                "            ,\"extractor\" : \"org.apache.metron.dataloads.extractor.ExtractorTest$DummyExtractor\"\n" +
                "         }";
        ExtractorHandler handler = ExtractorHandler.load(config);
        ThreatIntelResults results = handler.getExtractor().extract(null);
        Assert.assertEquals("dummy", results.getKey().indicator);
        Assert.assertEquals("dummy", results.getValue().get("indicator"));
    }
}
