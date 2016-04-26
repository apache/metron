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
package org.apache.metron.dataloads.extractor;

import com.google.common.collect.Iterables;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ExtractorTest {
    public static class DummyExtractor implements Extractor
    {

        @Override
        public Iterable<LookupKV> extract(String line) throws IOException {
            EnrichmentKey key = new EnrichmentKey();
            key.indicator = "dummy";
            key.type = "type";
            Map<String, String> value = new HashMap<>();
            value.put("indicator", "dummy");
            return Arrays.asList(new LookupKV(key, new EnrichmentValue(value)));
        }

        @Override
        public void initialize(Map<String, Object> config) {

        }
    }
    @Test
    public void testDummyExtractor() throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
        Extractor extractor = Extractors.create(DummyExtractor.class.getName());
        LookupKV results = Iterables.getFirst(extractor.extract(null), null);
        EnrichmentKey key = (EnrichmentKey) results.getKey();
        EnrichmentValue value = (EnrichmentValue) results.getValue();
        Assert.assertEquals("dummy", key.indicator);
        Assert.assertEquals("type", key.type);
        Assert.assertEquals("dummy", value.getMetadata().get("indicator"));
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
        LookupKV results = Iterables.getFirst(handler.getExtractor().extract(null), null);
        EnrichmentKey key = (EnrichmentKey) results.getKey();
        EnrichmentValue value = (EnrichmentValue) results.getValue();
        Assert.assertEquals("dummy", key.indicator);
        Assert.assertEquals("type", key.type);
        Assert.assertEquals("dummy", value.getMetadata().get("indicator"));
    }
}
