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
import org.apache.metron.dataloads.extractor.stix.types.URIHandler;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Test;
import org.mitre.cybox.objects.URIObjectType;

import java.util.HashMap;
import java.util.List;

public class URIHandlerTest {

  /**
   *<?xml version="1.0" encoding="UTF-8"?>
   *<stix:STIX_Package xmlns:stix="http://stix.mitre.org/stix-1"
   *                   xmlns:tdq="http://taxii.mitre.org/query/taxii_default_query-1"
   *                   xmlns:TOUMarking="http://data-marking.mitre.org/extensions/MarkingStructure#Terms_Of_Use-1"
   *                   xmlns:stixVocabs="http://stix.mitre.org/default_vocabularies-1"
   *                   xmlns:cyboxCommon="http://cybox.mitre.org/common-2"
   *                   xmlns:simpleMarking="http://data-marking.mitre.org/extensions/MarkingStructure#Simple-1"
   *                   xmlns:cyboxVocabs="http://cybox.mitre.org/default_vocabularies-2"
   *                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   *                   xmlns:stixCommon="http://stix.mitre.org/common-1"
   *                   xmlns:edge="http://soltra.com/"
   *                   xmlns:marking="http://data-marking.mitre.org/Marking-1"
   *                   xmlns:tlpMarking="http://data-marking.mitre.org/extensions/MarkingStructure#TLP-1"
   *                   xmlns:taxii_11="http://taxii.mitre.org/messages/taxii_xml_binding-1.1"
   *                   xmlns:opensource="http://hailataxii.com"
   *                   xmlns:URIObj="http://cybox.mitre.org/objects#URIObject-2"
   *                   xmlns:taxii="http://taxii.mitre.org/messages/taxii_xml_binding-1"
   *                   xmlns:cybox="http://cybox.mitre.org/cybox-2"
   *                   id="edge:Package-208ba7e1-ecc1-49a1-a96d-f28c4146761d"
   *                   timestamp="2017-08-09T21:05:27.148461+00:00"
   *                   version="1.1.1">
   *  <stix:STIX_Header>
   *    <stix:Handling>
   *      <marking:Marking>
   *        <marking:Controlled_Structure>../../../../descendant-or-self::node()</marking:Controlled_Structure>
   *        <marking:Marking_Structure color="WHITE" xsi:type="tlpMarking:TLPMarkingStructureType"/>
   *        <marking:Marking_Structure xsi:type="TOUMarking:TermsOfUseMarkingStructureType">
   *          <TOUMarking:Terms_Of_Use>TBD</TOUMarking:Terms_Of_Use>
   *        </marking:Marking_Structure>
   *        <marking:Marking_Structure xsi:type="simpleMarking:SimpleMarkingStructureType">
   *          <simpleMarking:Statement>Unclassified (Public)</simpleMarking:Statement>
   *        </marking:Marking_Structure>
   *      </marking:Marking>
   *    </stix:Handling>
   *  </stix:STIX_Header>
   *  <stix:Observables cybox_major_version="2" cybox_minor_version="1" cybox_update_version="0">
   *    <cybox:Observable id="opensource:Observable-6b98960f-c8bb-45fd-8b6d-8960e803b51f" sighting_count="1">
   *      <cybox:Title>URL: http://www.kotimi.com/alpha/gtex/...</cybox:Title>
   *      <cybox:Description>URL: http://www.kotimi.com/alpha/gtex/| isOnline:yes| dateVerified:2017-07-31T22:03:10+00:00</cybox:Description>
   *      <cybox:Object id="opensource:URI-9baf3b48-4aa2-4198-92b7-b5cb0a0a1d35">
   *        <cybox:Properties type="URL" xsi:type="URIObj:URIObjectType">
   *          <URIObj:Value condition="Equals">http://www.kotimi.com/alpha/gtex/</URIObj:Value>
   *        </cybox:Properties>
   *      </cybox:Object>
   *    </cybox:Observable>
   *  </stix:Observables>
   *</stix:STIX_Package>
   */
  @Multiline
  static String uriHandlerObject;

  @Test
  public void testURIHandler() throws Exception {
    StixExtractor extractor = new StixExtractor();
    extractor.initialize(new HashMap<>());
    Iterable<LookupKV> kvs = extractor.extract(uriHandlerObject);
    Assert.assertEquals(1, Iterables.size(kvs));
    LookupKV kv = Iterables.getFirst(kvs, null);
    EnrichmentKey key = (EnrichmentKey) kv.getKey();
    Assert.assertEquals("http://www.kotimi.com/alpha/gtex/", key.getIndicator());
    Assert.assertEquals("uriobjecttype", key.type);
  }
}
