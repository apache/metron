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
package org.apache.metron.dataloads.extractor.stix;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.nio.charset.StandardCharsets;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

public class StixExtractorTest {
  private String stixDoc;

  private String stixDocWithoutCondition;

  @Before
  public void setup() throws IOException {
    stixDoc = Joiner.on("\n").join(IOUtils.readLines(new InputStreamReader(new FileInputStream(new File("src/test/resources/stix_example.xml")),
        StandardCharsets.UTF_8)));
    stixDocWithoutCondition = Joiner.on("\n").join(IOUtils.readLines(new InputStreamReader(new FileInputStream(new File("src/test/resources/stix_example_wo_conditions.xml")), StandardCharsets.UTF_8)));
  }

  /**
   {
   "config" : {
   "stix_address_categories" : "IPV_4_ADDR"
   }
   ,"extractor" : "STIX"
   }
   */
  @Multiline
  private static String stixConfigOnlyIPV4;
  /**
   {
   "config" : {
   "stix_address_categories" : "IPV_6_ADDR"
   }
   ,"extractor" : "STIX"
   }
   */
  @Multiline
  private static String stixConfigOnlyIPV6;
  /**
   {
   "config" : {
   }
   ,"extractor" : "STIX"
   }
   */
  @Multiline
  private static String stixConfig;

  @Test
  public void testStixAddressesWithCondition() throws Exception {
    testStixAddresses(stixDoc);
  }

  @Test
  public void testStixAddressesWithoutCondition() throws Exception {
    testStixAddresses(stixDocWithoutCondition);
  }

  public void testStixAddresses(final String stixDoc) throws Exception {
    Thread t1 = new Thread( () ->
    {
      try {
        ExtractorHandler handler = ExtractorHandler.load(stixConfigOnlyIPV4);
        Extractor extractor = handler.getExtractor();
        Iterable<LookupKV> results = extractor.extract(stixDoc);

        Assert.assertEquals(3, Iterables.size(results));
        Assert.assertEquals("10.0.0.0", ((EnrichmentKey) (Iterables.get(results, 0).getKey())).indicator);
        Assert.assertEquals("10.0.0.1", ((EnrichmentKey) (Iterables.get(results, 1).getKey())).indicator);
        Assert.assertEquals("10.0.0.2", ((EnrichmentKey) (Iterables.get(results, 2).getKey())).indicator);
      }
      catch(Exception ex) {
        throw new RuntimeException(ex.getMessage(), ex);
      }
    });
    Thread t2 = new Thread( () ->
    {
      try {
        ExtractorHandler handler = ExtractorHandler.load(stixConfig);
        Extractor extractor = handler.getExtractor();
        Iterable<LookupKV> results = extractor.extract(stixDoc);
        Assert.assertEquals(3, Iterables.size(results));
        Assert.assertEquals("10.0.0.0", ((EnrichmentKey) (Iterables.get(results, 0).getKey())).indicator);
        Assert.assertEquals("10.0.0.1", ((EnrichmentKey) (Iterables.get(results, 1).getKey())).indicator);
        Assert.assertEquals("10.0.0.2", ((EnrichmentKey) (Iterables.get(results, 2).getKey())).indicator);
      }
      catch(Exception ex) {
        throw new RuntimeException(ex.getMessage(), ex);
      }
    });
    Thread t3 = new Thread( () ->
    {
      try {
        ExtractorHandler handler = ExtractorHandler.load(stixConfigOnlyIPV6);
        Extractor extractor = handler.getExtractor();
        Iterable<LookupKV> results = extractor.extract(stixDoc);
        Assert.assertEquals(0, Iterables.size(results));
      }
      catch(Exception ex) {
        throw new RuntimeException(ex.getMessage(), ex);
      }
    });
    t1.run();
    t2.run();
    t3.run();
    t1.join();
    t2.join();
    t3.join();
  }
}
