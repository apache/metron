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
package org.apache.metron.parsers.soltra;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SoltraParserTest {
    private SoltraParser sp = new SoltraParser();

    public SoltraParserTest() throws Exception {
        super();
    }


    @Test
    public void testIP() {
        String testString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><stix:STIX_Package id=\"edge:Package-ad14258b-5327-4ad7-be38-5f0f01f64331\" timestamp=\"2016-04-25T18:59:15.776987+00:00\" version=\"1.1.1\" xmlns:AddressObj=\"http://cybox.mitre.org/objects#AddressObject-2\" xmlns:TOUMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Terms_Of_Use-1\" xmlns:cybox=\"http://cybox.mitre.org/cybox-2\" xmlns:cyboxCommon=\"http://cybox.mitre.org/common-2\" xmlns:cyboxVocabs=\"http://cybox.mitre.org/default_vocabularies-2\" xmlns:edge=\"http://soltra.com/\" xmlns:marking=\"http://data-marking.mitre.org/Marking-1\" xmlns:opensource=\"http://hailataxii.com\" xmlns:simpleMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Simple-1\" xmlns:soltra=\"http://taxii.soltra.com/messages/taxii_extension_xml_binding-1.1\" xmlns:stix=\"http://stix.mitre.org/stix-1\" xmlns:stixCommon=\"http://stix.mitre.org/common-1\" xmlns:stixVocabs=\"http://stix.mitre.org/default_vocabularies-1\" xmlns:taxii=\"http://taxii.mitre.org/messages/taxii_xml_binding-1\" xmlns:tdq=\"http://taxii.mitre.org/query/taxii_default_query-1\" xmlns:tlpMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#TLP-1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <stix:STIX_Header> <stix:Handling> <marking:Marking> <marking:Controlled_Structure>../../../../descendant-or-self::node() | ../../../../descendant-or-self::node()/@*</marking:Controlled_Structure> <marking:Marking_Structure color=\"WHITE\" xsi:type=\"tlpMarking:TLPMarkingStructureType\"></marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"TOUMarking:TermsOfUseMarkingStructureType\"><TOUMarking:Terms_Of_Use>torstatus.blutmagie.de | http://torstatus.blutmagie.de - HailATaxii.com (HAT) has made a &apos;best effort&apos; attempt to find/determined the TOU (Term of Use) for this site&apos;s data, however none was found. - HAT assumes that attribution is a minimum requirement. - HAT assumes this data was created and owned by torstatus.blutmagie.de. - HAT has only modified the format of the data from CSV to STIX, and has not made any changes to the contains of the data as it was received at the time of conversion. </TOUMarking:Terms_Of_Use> </marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"simpleMarking:SimpleMarkingStructureType\"><simpleMarking:Statement>Unclassified (Public)</simpleMarking:Statement> </marking:Marking_Structure> </marking:Marking> </stix:Handling> </stix:STIX_Header> <stix:Observables cybox_major_version=\"2\" cybox_minor_version=\"1\" cybox_update_version=\"0\"><cybox:Observable id=\"opensource:Observable-88a253b1-d5e2-4ce7-9f78-d806dd8ee407\" sighting_count=\"1\"><cybox:Title>IP: 213.162.137.184</cybox:Title><cybox:Description>IPv4: 213.162.137.184 | isSource: True | </cybox:Description><cybox:Object id=\"opensource:Address-f2c468ba-8769-4616-b29a-bca4c5382020\"><cybox:Properties category=\"ipv4-addr\" is_source=\"true\" xsi:type=\"AddressObj:AddressObjectType\"><AddressObj:Address_Value condition=\"Equals\">213.162.137.184</AddressObj:Address_Value> </cybox:Properties> </cybox:Object> </cybox:Observable> </stix:Observables> </stix:STIX_Package>";

        List<JSONObject> result = sp.parse(testString.getBytes());

        JSONObject jo = result.get(0);

        assertEquals(jo.get("Edge_ID"), "edge:Package-ad14258b-5327-4ad7-be38-5f0f01f64331");
        assertEquals(jo.get("IP_Range_To"), "213.162.137.184");
        assertEquals(jo.get("IP_Range_From"), "213.162.137.184");
        assertEquals(jo.get("Type"), "IP");
        assertEquals(jo.get("ID"), "opensource:Observable-88a253b1-d5e2-4ce7-9f78-d806dd8ee407");
        assertEquals(jo.get("timestamp"), 1461610755000L);


        System.out.println(result);
    }


    @Test
    public void testDomain(){
        String testString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><stix:STIX_Package id=\"edge:Package-febb9b37-8bd4-4455-8140-efc97b6a211d\" timestamp=\"2016-04-25T18:59:18.503434+00:00\" version=\"1.1.1\" xmlns:DomainNameObj=\"http://cybox.mitre.org/objects#DomainNameObject-1\" xmlns:TOUMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Terms_Of_Use-1\" xmlns:cybox=\"http://cybox.mitre.org/cybox-2\" xmlns:cyboxCommon=\"http://cybox.mitre.org/common-2\" xmlns:cyboxVocabs=\"http://cybox.mitre.org/default_vocabularies-2\" xmlns:edge=\"http://soltra.com/\" xmlns:marking=\"http://data-marking.mitre.org/Marking-1\" xmlns:opensource=\"http://hailataxii.com\" xmlns:simpleMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Simple-1\" xmlns:soltra=\"http://taxii.soltra.com/messages/taxii_extension_xml_binding-1.1\" xmlns:stix=\"http://stix.mitre.org/stix-1\" xmlns:stixCommon=\"http://stix.mitre.org/common-1\" xmlns:stixVocabs=\"http://stix.mitre.org/default_vocabularies-1\" xmlns:taxii=\"http://taxii.mitre.org/messages/taxii_xml_binding-1\" xmlns:tdq=\"http://taxii.mitre.org/query/taxii_default_query-1\" xmlns:tlpMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#TLP-1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <stix:STIX_Header> <stix:Handling> <marking:Marking> <marking:Controlled_Structure>../../../../descendant-or-self::node() | ../../../../descendant-or-self::node()/@*</marking:Controlled_Structure> <marking:Marking_Structure color=\"WHITE\" xsi:type=\"tlpMarking:TLPMarkingStructureType\"></marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"TOUMarking:TermsOfUseMarkingStructureType\"><TOUMarking:Terms_Of_Use>torstatus.blutmagie.de | http://torstatus.blutmagie.de - HailATaxii.com (HAT) has made a &apos;best effort&apos; attempt to find/determined the TOU (Term of Use) for this site&apos;s data, however none was found. - HAT assumes that attribution is a minimum requirement. - HAT assumes this data was created and owned by torstatus.blutmagie.de. - HAT has only modified the format of the data from CSV to STIX, and has not made any changes to the contains of the data as it was received at the time of conversion. </TOUMarking:Terms_Of_Use> </marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"simpleMarking:SimpleMarkingStructureType\"><simpleMarking:Statement>Unclassified (Public)</simpleMarking:Statement> </marking:Marking_Structure> </marking:Marking> </stix:Handling> </stix:STIX_Header> <stix:Observables cybox_major_version=\"2\" cybox_minor_version=\"1\" cybox_update_version=\"0\"><cybox:Observable id=\"opensource:Observable-89644a41-b155-447d-9c87-71f862af5396\" sighting_count=\"1\"><cybox:Title>Domain: p5DDFA342.dip0.t-ipconnect.de</cybox:Title><cybox:Description>Domain: p5DDFA342.dip0.t-ipconnect.de | isFQDN: True | </cybox:Description><cybox:Object id=\"opensource:DomainName-598f49ec-6693-4fca-a2a8-ac59bc61ac06\"><cybox:Properties xsi:type=\"DomainNameObj:DomainNameObjectType\"><DomainNameObj:Value condition=\"Equals\">p5DDFA342.dip0.t-ipconnect.de</DomainNameObj:Value> </cybox:Properties> </cybox:Object> </cybox:Observable> </stix:Observables> </stix:STIX_Package>";


        List<JSONObject> result = sp.parse(testString.getBytes());

        JSONObject jo = result.get(0);

        assertEquals(jo.get("Edge_ID"), "edge:Package-febb9b37-8bd4-4455-8140-efc97b6a211d");
        assertEquals(jo.get("Type"), "Domain");
        assertEquals(jo.get("ID"), "opensource:Observable-89644a41-b155-447d-9c87-71f862af5396");
        assertEquals(jo.get("Domain"), "p5DDFA342.dip0.t-ipconnect.de");
        assertEquals(jo.get("timestamp"), 1461610758000L);


        System.out.println(result);
    }


    @Test
    public void testParent(){
        String testString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><stix:STIX_Package id=\"edge:Package-fee6363a-f421-4628-8922-188be722b3b9\" timestamp=\"2016-04-25T18:59:17.198795+00:00\" version=\"1.1.1\" xmlns:TOUMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Terms_Of_Use-1\" xmlns:cybox=\"http://cybox.mitre.org/cybox-2\" xmlns:cyboxCommon=\"http://cybox.mitre.org/common-2\" xmlns:cyboxVocabs=\"http://cybox.mitre.org/default_vocabularies-2\" xmlns:edge=\"http://soltra.com/\" xmlns:marking=\"http://data-marking.mitre.org/Marking-1\" xmlns:opensource=\"http://hailataxii.com\" xmlns:simpleMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Simple-1\" xmlns:soltra=\"http://taxii.soltra.com/messages/taxii_extension_xml_binding-1.1\" xmlns:stix=\"http://stix.mitre.org/stix-1\" xmlns:stixCommon=\"http://stix.mitre.org/common-1\" xmlns:stixVocabs=\"http://stix.mitre.org/default_vocabularies-1\" xmlns:taxii=\"http://taxii.mitre.org/messages/taxii_xml_binding-1\" xmlns:tdq=\"http://taxii.mitre.org/query/taxii_default_query-1\" xmlns:tlpMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#TLP-1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <stix:STIX_Header> <stix:Handling> <marking:Marking> <marking:Controlled_Structure>../../../../descendant-or-self::node() | ../../../../descendant-or-self::node()/@*</marking:Controlled_Structure> <marking:Marking_Structure color=\"WHITE\" xsi:type=\"tlpMarking:TLPMarkingStructureType\"></marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"TOUMarking:TermsOfUseMarkingStructureType\"><TOUMarking:Terms_Of_Use>torstatus.blutmagie.de | http://torstatus.blutmagie.de - HailATaxii.com (HAT) has made a &apos;best effort&apos; attempt to find/determined the TOU (Term of Use) for this site&apos;s data, however none was found. - HAT assumes that attribution is a minimum requirement. - HAT assumes this data was created and owned by torstatus.blutmagie.de. - HAT has only modified the format of the data from CSV to STIX, and has not made any changes to the contains of the data as it was received at the time of conversion. </TOUMarking:Terms_Of_Use> </marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"simpleMarking:SimpleMarkingStructureType\"><simpleMarking:Statement>Unclassified (Public)</simpleMarking:Statement> </marking:Marking_Structure> </marking:Marking> </stix:Handling> </stix:STIX_Header> <stix:Observables cybox_major_version=\"2\" cybox_minor_version=\"1\" cybox_update_version=\"0\"><cybox:Observable id=\"opensource:Observable-485fbe42-5365-45a7-b19b-685a808ffc72\"><cybox:Observable_Composition operator=\"OR\"><cybox:Observable idref=\"opensource:Observable-4782a6ee-4018-4cb7-967e-8c95de01426f\"> </cybox:Observable><cybox:Observable idref=\"opensource:Observable-49339bc9-f2d0-4de2-b14b-6ef04f110169\"> </cybox:Observable><cybox:Observable idref=\"opensource:Observable-238bf5dd-e1d1-4d69-b081-45a63e1266cc\"> </cybox:Observable> </cybox:Observable_Composition> </cybox:Observable> </stix:Observables> </stix:STIX_Package>";


        List<JSONObject> result = sp.parse(testString.getBytes());

        JSONObject jo = result.get(0);

        assertEquals(jo.get("Edge_ID"), "edge:Package-fee6363a-f421-4628-8922-188be722b3b9");

        assertEquals(jo.get("Type"), "Parent");
        assertEquals(jo.get("ID"), "opensource:Observable-485fbe42-5365-45a7-b19b-685a808ffc72");
        assertEquals(((JSONArray)jo.get("Children")).get(0), "opensource:Observable-4782a6ee-4018-4cb7-967e-8c95de01426f");
        assertEquals(((JSONArray)jo.get("Children")).get(1), "opensource:Observable-49339bc9-f2d0-4de2-b14b-6ef04f110169");
        assertEquals(((JSONArray)jo.get("Children")).get(2), "opensource:Observable-238bf5dd-e1d1-4d69-b081-45a63e1266cc");
        assertEquals(jo.get("timestamp"), 1461610757000L);

        System.out.println(result);
    }

    @Test
    public void testIndicator(){
        String testString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><stix:STIX_Package id=\"edge:Package-ca54fdf2-a012-4ba5-b9b6-640f091e4829\" timestamp=\"2016-04-25T18:45:02.803705+00:00\" version=\"1.1.1\" xmlns:TOUMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Terms_Of_Use-1\" xmlns:cybox=\"http://cybox.mitre.org/cybox-2\" xmlns:cyboxCommon=\"http://cybox.mitre.org/common-2\" xmlns:cyboxVocabs=\"http://cybox.mitre.org/default_vocabularies-2\" xmlns:edge=\"http://soltra.com/\" xmlns:indicator=\"http://stix.mitre.org/Indicator-2\" xmlns:marking=\"http://data-marking.mitre.org/Marking-1\" xmlns:opensource=\"http://hailataxii.com\" xmlns:simpleMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Simple-1\" xmlns:soltra=\"http://taxii.soltra.com/messages/taxii_extension_xml_binding-1.1\" xmlns:stix=\"http://stix.mitre.org/stix-1\" xmlns:stixCommon=\"http://stix.mitre.org/common-1\" xmlns:stixVocabs=\"http://stix.mitre.org/default_vocabularies-1\" xmlns:taxii=\"http://taxii.mitre.org/messages/taxii_xml_binding-1\" xmlns:tdq=\"http://taxii.mitre.org/query/taxii_default_query-1\" xmlns:tlpMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#TLP-1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <stix:STIX_Header> <stix:Handling> <marking:Marking> <marking:Controlled_Structure>../../../../descendant-or-self::node() | ../../../../descendant-or-self::node()/@*</marking:Controlled_Structure> <marking:Marking_Structure color=\"WHITE\" xsi:type=\"tlpMarking:TLPMarkingStructureType\"></marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"TOUMarking:TermsOfUseMarkingStructureType\"><TOUMarking:Terms_Of_Use>torstatus.blutmagie.de | http://torstatus.blutmagie.de - HailATaxii.com (HAT) has made a &apos;best effort&apos; attempt to find/determined the TOU (Term of Use) for this site&apos;s data, however none was found. - HAT assumes that attribution is a minimum requirement. - HAT assumes this data was created and owned by torstatus.blutmagie.de. - HAT has only modified the format of the data from CSV to STIX, and has not made any changes to the contains of the data as it was received at the time of conversion. </TOUMarking:Terms_Of_Use> </marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"simpleMarking:SimpleMarkingStructureType\"><simpleMarking:Statement>Unclassified (Public)</simpleMarking:Statement> </marking:Marking_Structure> </marking:Marking> </stix:Handling> </stix:STIX_Header> <stix:Indicators><stix:Indicator id=\"opensource:indicator-71ad58e0-2b79-4602-8376-206515a55c7b\" timestamp=\"2016-04-25T02:05:54.271214+00:00\" version=\"2.1.1\" xsi:type=\"indicator:IndicatorType\"><indicator:Title> This domain 194-166-133-146.adsl.highway.telekom.at has been identified as a TOR network &quot;Exit Point&quot; router</indicator:Title><indicator:Type xsi:type=\"stixVocabs:IndicatorTypeVocab-1.1\">IP Watchlist</indicator:Type><indicator:Type xsi:type=\"stixVocabs:IndicatorTypeVocab-1.1\">Domain Watchlist</indicator:Type><indicator:Description> torstatus.blutmagie.de has identified this domain 194-166-133-146.adsl.highway.telekom.at as a TOR network &quot;Exit Point&quot; router, which appears to be located in Austria. RawData: {&apos;OrAddress&apos;: u&apos;None&apos;, &apos;IP Address&apos;: u&apos;194.166.133.146&apos;, &apos;ConsensusBandwidth&apos;: 27, &apos;Hostname&apos;: u&apos;194-166-133-146.adsl.highway.telekom.at&apos;, &apos;Uptime (Hours)&apos;: 3, &apos;Country Code&apos;: u&apos;AT&apos;, &apos;Router Name&apos;: u&apos;Unnamed&apos;, &apos;Bandwidth (KB/s)&apos;: 3, &apos;Platform&apos;: u&apos;Tor 0.2.4.23 on Linux&apos;, &apos;ASNumber&apos;: u&apos;1901&apos;, &apos;Flags&apos;: {&apos;Flag - Named&apos;: 0, &apos;Flag - Stable&apos;: 0, &apos;Flag - Bad Exit&apos;: 0, &apos;Flag - Authority&apos;: 0, &apos;Flag - Valid&apos;: 1, &apos;Flag - Guard&apos;: 0, &apos;Flag - Hibernating&apos;: 0, &apos;Flag - Fast&apos;: 0, &apos;Flag - Running&apos;: 1, &apos;Flag - Exit&apos;: 0, &apos;Flag - V2Dir&apos;: 1}, &apos;ASName&apos;: u&apos;EUNETAT-AS A1 Telekom Austria AG- AT&apos;, &apos;Ports&apos;: {&apos;ORPort&apos;: 8444, &apos;DirPort&apos;: 8445}, &apos;FirstSeen&apos;: u&apos;2014-10-17&apos;}</indicator:Description><indicator:Observable idref=\"opensource:Observable-da228981-2b95-452c-a92b-378aa1555ddf\"> </indicator:Observable><indicator:Producer><stixCommon:Identity id=\"opensource:Identity-01e6d993-c72f-45db-a321-5af2e3b580a6\"><stixCommon:Name>torstatus.blutmagie.de</stixCommon:Name> </stixCommon:Identity><stixCommon:Time><cyboxCommon:Received_Time>2016-04-25T02:05:34+00:00</cyboxCommon:Received_Time> </stixCommon:Time> </indicator:Producer> </stix:Indicator> </stix:Indicators> </stix:STIX_Package>";


        List<JSONObject> result = sp.parse(testString.getBytes());

        JSONObject jo = result.get(0);

        assertEquals(jo.get("Edge_ID"), "edge:Package-ca54fdf2-a012-4ba5-b9b6-640f091e4829");

        assertEquals(jo.get("Type"), "Indicator");
        assertEquals(jo.get("ID"), "opensource:indicator-71ad58e0-2b79-4602-8376-206515a55c7b");
        assertEquals(jo.get("Child"), "opensource:Observable-da228981-2b95-452c-a92b-378aa1555ddf");
        assertEquals(jo.get("Producer"), "opensource:Identity-01e6d993-c72f-45db-a321-5af2e3b580a6");
        assertEquals(jo.get("Title"), "This domain 194-166-133-146.adsl.highway.telekom.at has been identified as a TOR network \"Exit Point\" router");
        assertEquals(jo.get("Description"), "torstatus.blutmagie.de has identified this domain 194-166-133-146.adsl.highway.telekom.at as a TOR network \"Exit Point\" router, which appears to be located in Austria. RawData: {'OrAddress': u'None', 'IP Address': u'194.166.133.146', 'ConsensusBandwidth': 27, 'Hostname': u'194-166-133-146.adsl.highway.telekom.at', 'Uptime (Hours)': 3, 'Country Code': u'AT', 'Router Name': u'Unnamed', 'Bandwidth (KB/s)': 3, 'Platform': u'Tor 0.2.4.23 on Linux', 'ASNumber': u'1901', 'Flags': {'Flag - Named': 0, 'Flag - Stable': 0, 'Flag - Bad Exit': 0, 'Flag - Authority': 0, 'Flag - Valid': 1, 'Flag - Guard': 0, 'Flag - Hibernating': 0, 'Flag - Fast': 0, 'Flag - Running': 1, 'Flag - Exit': 0, 'Flag - V2Dir': 1}, 'ASName': u'EUNETAT-AS A1 Telekom Austria AG- AT', 'Ports': {'ORPort': 8444, 'DirPort': 8445}, 'FirstSeen': u'2014-10-17'}");

        assertEquals(jo.get("timestamp"), 1461609902000L);

        System.out.println(result);
    }

    @Test
    public void testTTP(){
        String testString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><stix:STIX_Package id=\"edge:Package-59c9af43-faab-47a1-8559-dc8153b9b607\" timestamp=\"2016-04-25T18:37:55.431374+00:00\" version=\"1.1.1\" xmlns:TOUMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Terms_Of_Use-1\" xmlns:cybox=\"http://cybox.mitre.org/cybox-2\" xmlns:cyboxCommon=\"http://cybox.mitre.org/common-2\" xmlns:cyboxVocabs=\"http://cybox.mitre.org/default_vocabularies-2\" xmlns:edge=\"http://soltra.com/\" xmlns:marking=\"http://data-marking.mitre.org/Marking-1\" xmlns:opensource=\"http://hailataxii.com\" xmlns:simpleMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#Simple-1\" xmlns:soltra=\"http://taxii.soltra.com/messages/taxii_extension_xml_binding-1.1\" xmlns:stix=\"http://stix.mitre.org/stix-1\" xmlns:stixCommon=\"http://stix.mitre.org/common-1\" xmlns:stixVocabs=\"http://stix.mitre.org/default_vocabularies-1\" xmlns:taxii=\"http://taxii.mitre.org/messages/taxii_xml_binding-1\" xmlns:tdq=\"http://taxii.mitre.org/query/taxii_default_query-1\" xmlns:tlpMarking=\"http://data-marking.mitre.org/extensions/MarkingStructure#TLP-1\" xmlns:ttp=\"http://stix.mitre.org/TTP-1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <stix:STIX_Header> <stix:Handling> <marking:Marking> <marking:Controlled_Structure>../../../../descendant-or-self::node() | ../../../../descendant-or-self::node()/@*</marking:Controlled_Structure> <marking:Marking_Structure color=\"WHITE\" xsi:type=\"tlpMarking:TLPMarkingStructureType\"></marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"TOUMarking:TermsOfUseMarkingStructureType\"><TOUMarking:Terms_Of_Use>www.malwaredomainlist.com | Malware Domain List - is a non-commercial community project. Our list can be used for free by anyone. Feel free to use it. </TOUMarking:Terms_Of_Use> </marking:Marking_Structure> <marking:Marking_Structure xsi:type=\"simpleMarking:SimpleMarkingStructureType\"><simpleMarking:Statement>Unclassified (Public)</simpleMarking:Statement> </marking:Marking_Structure> </marking:Marking> </stix:Handling> </stix:STIX_Header> <stix:TTPs><stix:TTP id=\"opensource:ttp-148dc95f-e747-46fc-90f0-643e531cdab4\" timestamp=\"2016-04-20T15:07:33.780616+00:00\" xsi:type=\"ttp:TTPType\"><ttp:Title>gateway to Angler EK</ttp:Title><ttp:Behavior><ttp:Malware><ttp:Malware_Instance id=\"opensource:malware-d9b8a92e-f41b-4dc5-96d7-a2cd1d127d42\"><ttp:Type xsi:type=\"stixVocabs:MalwareTypeVocab-1.0\">Remote Access Trojan</ttp:Type> </ttp:Malware_Instance> </ttp:Malware> </ttp:Behavior> </stix:TTP> </stix:TTPs> </stix:STIX_Package>";


        List<JSONObject> result = sp.parse(testString.getBytes());

        JSONObject jo = result.get(0);

        assertEquals(jo.get("Edge_ID"), "edge:Package-59c9af43-faab-47a1-8559-dc8153b9b607");

        assertEquals(jo.get("Type"), "TTP");
        assertEquals(jo.get("ID"), "opensource:ttp-148dc95f-e747-46fc-90f0-643e531cdab4");
        assertEquals(jo.get("Title"), "gateway to Angler EK");
        assertEquals(jo.get("Malware_Identity"), "opensource:malware-d9b8a92e-f41b-4dc5-96d7-a2cd1d127d42");

        assertEquals(jo.get("timestamp"), 1461609475000L);

        System.out.println(result);

    }
}
