package org.apache.metron.dataloads.extractor;

import org.apache.metron.dataloads.hbase.ThreatIntelKey;
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
        public ExtractorResults extract(String line) throws IOException {
            ThreatIntelKey key = new ThreatIntelKey();
            key.indicator = "dummy";
            Map<String, String> value = new HashMap<>();
            value.put("indicator", "dummy");
            return new ExtractorResults(key, value);
        }

        @Override
        public void initialize(Map<String, Object> config) {

        }
    }
    @Test
    public void testDummyExtractor() throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
        Extractor extractor = Extractors.create(DummyExtractor.class.getName());
        ExtractorResults results = extractor.extract(null);
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
        ExtractorResults results = handler.getExtractor().extract(null);
        Assert.assertEquals("dummy", results.getKey().indicator);
        Assert.assertEquals("dummy", results.getValue().get("indicator"));
    }
}
