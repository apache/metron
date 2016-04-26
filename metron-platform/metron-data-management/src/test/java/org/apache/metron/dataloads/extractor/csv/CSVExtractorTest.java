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
package org.apache.metron.dataloads.extractor.csv;

import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CSVExtractorTest {

  /**
   {
     "config" : {
        "columns" : {
            "host" : 0
           ,"meta" : 2
                    }
       ,"indicator_column" : "host"
       ,"type" : "threat"
       ,"separator" : ","
               }
     ,"extractor" : "CSV"
   }
   */
  @Multiline
  static String testCSVConfig;

  @Test
  public void testCSVExtractor() throws Exception {

    ExtractorHandler handler = ExtractorHandler.load(testCSVConfig);
    validate(handler);
  }

  public void validate(ExtractorHandler handler) throws IOException {
    {
      LookupKV results = Iterables.getFirst(handler.getExtractor().extract("google.com,1.0,foo"), null);
      EnrichmentKey key = (EnrichmentKey) results.getKey();
      EnrichmentValue value = (EnrichmentValue) results.getValue();
      Assert.assertEquals("google.com", key.indicator);
      Assert.assertEquals("threat", key.type);
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
