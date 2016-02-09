package org.apache.metron.dataloads.extractor;

import com.google.common.collect.Iterables;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cstella on 2/3/16.
 */
public class ExtractorTest {
    public static class DummyExtractor implements Extractor
    {

        @Override
        public Iterable<ThreatIntelResults> extract(String line) throws IOException {
            ThreatIntelKey key = new ThreatIntelKey();
            key.indicator = "dummy";
            Map<String, String> value = new HashMap<>();
            value.put("indicator", "dummy");
            return Arrays.asList(new ThreatIntelResults(key, value));
        }

        @Override
        public void initialize(Map<String, Object> config) {

        }
    }
    @Test
    public void testDummyExtractor() throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
        Extractor extractor = Extractors.create(DummyExtractor.class.getName());
        ThreatIntelResults results = Iterables.getFirst(extractor.extract(null), null);
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
        ThreatIntelResults results = Iterables.getFirst(handler.getExtractor().extract(null), null);
        Assert.assertEquals("dummy", results.getKey().indicator);
        Assert.assertEquals("dummy", results.getValue().get("indicator"));
    }
}
