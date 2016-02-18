package org.apache.metron.dataloads.extractor.csv;

import com.google.common.collect.Iterables;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by cstella on 2/3/16.
 */
public class CSVExtractorTest {
    @Test
    public void testCSVExtractorMapColumns() throws Exception {
        /**
         {
            "config" : {
                        "columns" : {
                                "host" : 0
                                ,"meta" : 2
                                    }
                       ,"indicator_column" : "host"
                       ,"separator" : ","
                       }
            ,"extractor" : "CSV"
         }
         */
        String config = "{\n" +
                "            \"config\" : {\n" +
                "                        \"columns\" : [\"host:0\",\"meta:2\"]\n" +
                "                       ,\"indicator_column\" : \"host\"\n" +
                "                       ,\"separator\" : \",\"\n" +
                "                       }\n" +
                "            ,\"extractor\" : \"CSV\"\n" +
                "         }";
        ExtractorHandler handler = ExtractorHandler.load(config);
        validate(handler);
    }
    @Test
    public void testCSVExtractorListColumns() throws Exception {
        /**
         {
            "config" : {
                        "columns" : ["host:0","meta:2"]
                       ,"indicator_column" : "host"
                       ,"separator" : ","
                       }
            ,"extractor" : "CSV"
         }
         */
        String config = "{\n" +
                "            \"config\" : {\n" +
                "                        \"columns\" : [\"host:0\",\"meta:2\"]\n" +
                "                       ,\"indicator_column\" : \"host\"\n" +
                "                       ,\"separator\" : \",\"\n" +
                "                       }\n" +
                "            ,\"extractor\" : \"CSV\"\n" +
                "         }";
        ExtractorHandler handler = ExtractorHandler.load(config);
        validate(handler);
    }

    @Test
    public void testCSVExtractor() throws Exception {
        /**
         {
            "config" : {
                        "columns" : "host:0,meta:2"
                       ,"indicator_column" : "host"
                       ,"separator" : ","
                       }
            ,"extractor" : "CSV"
         }
         */
        String config = "{\n" +
                "            \"config\" : {\n" +
                "                        \"columns\" : \"host:0,meta:2\"\n" +
                "                       ,\"indicator_column\" : \"host\"\n" +
                "                       ,\"separator\" : \",\"\n" +
                "                       }\n" +
                "            ,\"extractor\" : \"CSV\"\n" +
                "         }";
        ExtractorHandler handler = ExtractorHandler.load(config);
        validate(handler);
    }

    public void validate(ExtractorHandler handler) throws IOException {
        {
            LookupKV results = Iterables.getFirst(handler.getExtractor().extract("google.com,1.0,foo"), null);
            ThreatIntelKey key = (ThreatIntelKey) results.getKey();
            ThreatIntelValue value = (ThreatIntelValue) results.getValue();
            Assert.assertEquals("google.com", key.indicator);
            Assert.assertEquals("google.com", value.getMetadata().get("host"));
            Assert.assertEquals("foo", value.getMetadata().get("meta"));
            Assert.assertEquals(2, value.getMetadata().size());
        }
        {
            Iterable<LookupKV> results = handler.getExtractor().extract("#google.com,1.0,foo");
            Assert.assertEquals(0, Iterables.size(results));
        }
    }
}
