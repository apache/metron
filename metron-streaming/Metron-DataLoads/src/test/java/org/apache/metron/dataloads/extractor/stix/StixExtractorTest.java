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

import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.hbase.converters.enrichment.EnrichmentKey;
import org.apache.metron.reference.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Test;

public class StixExtractorTest {
    /**
         <!--
         STIX IP Watchlist Example

         Copyright (c) 2015, The MITRE Corporation. All rights reserved.
         The contents of this file are subject to the terms of the STIX License located at http://stix.mitre.org/about/termsofuse.html.

         This example demonstrates a simple usage of STIX to represent a list of IP address indicators (watchlist of IP addresses). Cyber operations and malware analysis centers often share a list of suspected malicious IP addresses with information about what those IPs might indicate. This STIX package represents a list of three IP addresses with a short dummy description of what they represent.

         It demonstrates the use of:

         * STIX Indicators
         * CybOX within STIX
         * The CybOX Address Object (IP)
         * CybOX Patterns (apply_condition="ANY")
         * Controlled vocabularies

         Created by Mark Davidson
         -->
         <stix:STIX_Package
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:stix="http://stix.mitre.org/stix-1"
         xmlns:indicator="http://stix.mitre.org/Indicator-2"
         xmlns:cybox="http://cybox.mitre.org/cybox-2"
         xmlns:AddressObject="http://cybox.mitre.org/objects#AddressObject-2"
         xmlns:cyboxVocabs="http://cybox.mitre.org/default_vocabularies-2"
         xmlns:stixVocabs="http://stix.mitre.org/default_vocabularies-1"
         xmlns:example="http://example.com/"
         id="example:STIXPackage-33fe3b22-0201-47cf-85d0-97c02164528d"
         timestamp="2014-05-08T09:00:00.000000Z"
         version="1.2">
         <stix:STIX_Header>
         <stix:Title>Example watchlist that contains IP information.</stix:Title>
         <stix:Package_Intent xsi:type="stixVocabs:PackageIntentVocab-1.0">Indicators - Watchlist</stix:Package_Intent>
         </stix:STIX_Header>
         <stix:Indicators>
         <stix:Indicator xsi:type="indicator:IndicatorType" id="example:Indicator-33fe3b22-0201-47cf-85d0-97c02164528d" timestamp="2014-05-08T09:00:00.000000Z">
         <indicator:Type xsi:type="stixVocabs:IndicatorTypeVocab-1.1">IP Watchlist</indicator:Type>
         <indicator:Description>Sample IP Address Indicator for this watchlist. This contains one indicator with a set of three IP addresses in the watchlist.</indicator:Description>
         <indicator:Observable  id="example:Observable-1c798262-a4cd-434d-a958-884d6980c459">
         <cybox:Object id="example:Object-1980ce43-8e03-490b-863a-ea404d12242e">
         <cybox:Properties xsi:type="AddressObject:AddressObjectType" category="ipv4-addr">
         <AddressObject:Address_Value condition="Equals" apply_condition="ANY">10.0.0.0##comma##10.0.0.1##comma##10.0.0.2</AddressObject:Address_Value>
         </cybox:Properties>
         </cybox:Object>
         </indicator:Observable>
         </stix:Indicator>
         </stix:Indicators>
         </stix:STIX_Package>
         */
    @Multiline
    private static String stixDoc;

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
    public void testStixAddresses() throws Exception {
        {
            ExtractorHandler handler = ExtractorHandler.load(stixConfigOnlyIPV4);
            Extractor extractor = handler.getExtractor();
            Iterable<LookupKV> results = extractor.extract(stixDoc);

            Assert.assertEquals(3, Iterables.size(results));
            Assert.assertEquals("10.0.0.0", ((EnrichmentKey)(Iterables.get(results, 0).getKey())).indicator);
            Assert.assertEquals("10.0.0.1", ((EnrichmentKey)(Iterables.get(results, 1).getKey())).indicator);
            Assert.assertEquals("10.0.0.2", ((EnrichmentKey)(Iterables.get(results, 2).getKey())).indicator);
        }
        {

            ExtractorHandler handler = ExtractorHandler.load(stixConfig);
            Extractor extractor = handler.getExtractor();
            Iterable<LookupKV> results = extractor.extract(stixDoc);
            Assert.assertEquals(3, Iterables.size(results));
            Assert.assertEquals("10.0.0.0", ((EnrichmentKey)(Iterables.get(results, 0).getKey())).indicator);
            Assert.assertEquals("10.0.0.1", ((EnrichmentKey)(Iterables.get(results, 1).getKey())).indicator);
            Assert.assertEquals("10.0.0.2", ((EnrichmentKey)(Iterables.get(results, 2).getKey())).indicator);
        }
        {

            ExtractorHandler handler = ExtractorHandler.load(stixConfigOnlyIPV6);
            Extractor extractor = handler.getExtractor();
            Iterable<LookupKV> results = extractor.extract(stixDoc);
            Assert.assertEquals(0, Iterables.size(results));
        }
    }
}
