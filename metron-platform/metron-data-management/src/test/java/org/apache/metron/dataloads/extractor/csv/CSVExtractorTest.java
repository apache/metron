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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  public void testInitialize() throws Exception {
    CSVExtractor ex = new CSVExtractor();
    ExtractorHandler handler = ExtractorHandler.load(testCSVConfig);
    ex.initialize(handler.getConfig());

    assertEquals(0, (int)ex.getColumnMap().get("host") );
    assertEquals(2, (int)ex.getColumnMap().get("meta") );
    assertEquals(0, ex.getTypeColumnIndex() );
    assertEquals(0, ex.getIndicatorColumn());
    assertEquals("threat", ex.getType() );
    assertEquals(',', ex.getParser().getSeparator());

  }

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
      assertEquals("google.com", key.indicator);
      assertEquals("threat", key.type);
      assertEquals("google.com", value.getMetadata().get("host"));
      assertEquals("foo", value.getMetadata().get("meta"));
      assertEquals(2, value.getMetadata().size());
    }
    {
      Iterable<LookupKV> results = handler.getExtractor().extract("#google.com,1.0,foo");
      assertEquals(0, Iterables.size(results));
    }
    {
      Iterable<LookupKV> results = handler.getExtractor().extract("");
      assertEquals(0, Iterables.size(results));
    }
    {
      Iterable<LookupKV> results = handler.getExtractor().extract(" ");
      assertEquals(0, Iterables.size(results));
    }
    {
      Iterable<LookupKV> results = handler.getExtractor().extract(null);
      assertEquals(0, Iterables.size(results));
    }
  }
}
