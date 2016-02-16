package org.apache.metron.dataloads.extractor.stix;

import com.google.common.collect.Iterables;
import org.apache.metron.dataloads.ThreatIntelBulkLoader;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by cstella on 2/9/16.
 */
public class StixExtractorTest {
    @Test
    public void testStixAddresses() throws Exception {
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
        String stixDoc = "<!--\n" +
                "STIX IP Watchlist Example\n" +
                "\n" +
                "Copyright (c) 2015, The MITRE Corporation. All rights reserved.\n" +
                "The contents of this file are subject to the terms of the STIX License located at http://stix.mitre.org/about/termsofuse.html.\n" +
                "\n" +
                "This example demonstrates a simple usage of STIX to represent a list of IP address indicators (watchlist of IP addresses). Cyber operations and malware analysis centers often share a list of suspected malicious IP addresses with information about what those IPs might indicate. This STIX package represents a list of three IP addresses with a short dummy description of what they represent.\n" +
                "\n" +
                "It demonstrates the use of:\n" +
                "\n" +
                "* STIX Indicators\n" +
                "* CybOX within STIX\n" +
                "* The CybOX Address Object (IP)\n" +
                "* CybOX Patterns (apply_condition=\"ANY\")\n" +
                "* Controlled vocabularies\n" +
                "\n" +
                "Created by Mark Davidson\n" +
                "-->\n" +
                "<stix:STIX_Package\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xmlns:stix=\"http://stix.mitre.org/stix-1\"\n" +
                "    xmlns:indicator=\"http://stix.mitre.org/Indicator-2\"\n" +
                "    xmlns:cybox=\"http://cybox.mitre.org/cybox-2\"\n" +
                "    xmlns:AddressObject=\"http://cybox.mitre.org/objects#AddressObject-2\"\n" +
                "    xmlns:cyboxVocabs=\"http://cybox.mitre.org/default_vocabularies-2\"\n" +
                "    xmlns:stixVocabs=\"http://stix.mitre.org/default_vocabularies-1\"\n" +
                "    xmlns:example=\"http://example.com/\"\n" +
                "    id=\"example:STIXPackage-33fe3b22-0201-47cf-85d0-97c02164528d\"\n" +
                "    timestamp=\"2014-05-08T09:00:00.000000Z\"\n" +
                "    version=\"1.2\">\n" +
                "    <stix:STIX_Header>\n" +
                "        <stix:Title>Example watchlist that contains IP information.</stix:Title>\n" +
                "        <stix:Package_Intent xsi:type=\"stixVocabs:PackageIntentVocab-1.0\">Indicators - Watchlist</stix:Package_Intent>\n" +
                "    </stix:STIX_Header>\n" +
                "    <stix:Indicators>\n" +
                "        <stix:Indicator xsi:type=\"indicator:IndicatorType\" id=\"example:Indicator-33fe3b22-0201-47cf-85d0-97c02164528d\" timestamp=\"2014-05-08T09:00:00.000000Z\">\n" +
                "            <indicator:Type xsi:type=\"stixVocabs:IndicatorTypeVocab-1.1\">IP Watchlist</indicator:Type>\n" +
                "            <indicator:Description>Sample IP Address Indicator for this watchlist. This contains one indicator with a set of three IP addresses in the watchlist.</indicator:Description>\n" +
                "            <indicator:Observable  id=\"example:Observable-1c798262-a4cd-434d-a958-884d6980c459\">\n" +
                "                <cybox:Object id=\"example:Object-1980ce43-8e03-490b-863a-ea404d12242e\">\n" +
                "                    <cybox:Properties xsi:type=\"AddressObject:AddressObjectType\" category=\"ipv4-addr\">\n" +
                "                        <AddressObject:Address_Value condition=\"Equals\" apply_condition=\"ANY\">10.0.0.0##comma##10.0.0.1##comma##10.0.0.2</AddressObject:Address_Value>\n" +
                "                    </cybox:Properties>\n" +
                "                </cybox:Object>\n" +
                "            </indicator:Observable>\n" +
                "        </stix:Indicator>\n" +
                "    </stix:Indicators>\n" +
                "</stix:STIX_Package>\n" +
                "\n";
        {
            /**
             {
             "config" : {
             "stix_address_categories" : "IPV_4_ADDR"
             }
             ,"extractor" : "STIX"
             }
             */
            String config = "{\n" +
                    "            \"config\" : {\n" +
                    "                       \"stix_address_categories\" : \"IPV_4_ADDR\"\n" +
                    "                       }\n" +
                    "            ,\"extractor\" : \"STIX\"\n" +
                    "         }";
            ExtractorHandler handler = ExtractorHandler.load(config);
            Extractor extractor = handler.getExtractor();
            Iterable<ThreatIntelResults> results = extractor.extract(stixDoc);
            Assert.assertEquals(3, Iterables.size(results));
            Assert.assertEquals("10.0.0.0", Iterables.get(results, 0).getKey().indicator);
            Assert.assertEquals("10.0.0.1", Iterables.get(results, 1).getKey().indicator);
            Assert.assertEquals("10.0.0.2", Iterables.get(results, 2).getKey().indicator);
        }
        {
            /**
             {
             "config" : {
             }
             ,"extractor" : "STIX"
             }
             */
            String config = "{\n" +
                    "            \"config\" : {\n" +
                    "                       }\n" +
                    "            ,\"extractor\" : \"STIX\"\n" +
                    "         }";
            ExtractorHandler handler = ExtractorHandler.load(config);
            Extractor extractor = handler.getExtractor();
            Iterable<ThreatIntelResults> results = extractor.extract(stixDoc);
            Assert.assertEquals(3, Iterables.size(results));
            Assert.assertEquals("10.0.0.0", Iterables.get(results, 0).getKey().indicator);
            Assert.assertEquals("10.0.0.1", Iterables.get(results, 1).getKey().indicator);
            Assert.assertEquals("10.0.0.2", Iterables.get(results, 2).getKey().indicator);
        }
        {
            /**
             {
             "config" : {
                "stix_address_categories" : "IPV_6_ADDR"
             }
             ,"extractor" : "STIX"
             }
             */
            String config = "{\n" +
                    "            \"config\" : {\n" +
                    "                       \"stix_address_categories\" : \"IPV_6_ADDR\"\n" +
                    "                       }\n" +
                    "            ,\"extractor\" : \"STIX\"\n" +
                    "         }";
            ExtractorHandler handler = ExtractorHandler.load(config);
            Extractor extractor = handler.getExtractor();
            Iterable<ThreatIntelResults> results = extractor.extract(stixDoc);
            Assert.assertEquals(0, Iterables.size(results));
        }
    }
}
