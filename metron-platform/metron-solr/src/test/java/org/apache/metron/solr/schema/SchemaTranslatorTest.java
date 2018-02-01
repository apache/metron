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
package org.apache.metron.solr.schema;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.metron.solr.writer.SolrWriter;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class SchemaTranslatorTest {

  /**
{"adapter.threatinteladapter.end.ts":"1517499201357","bro_timestamp":"1517499194.7338","ip_dst_port":8080,"enrichmentsplitterbolt.splitter.end.ts":"1517499201202","enrichmentsplitterbolt.splitter.begin.ts":"1517499201200","adapter.hostfromjsonlistadapter.end.ts":"1517499201207","adapter.geoadapter.begin.ts":"1517499201209","uid":"CUrRne3iLIxXavQtci","trans_depth":143,"protocol":"http","original_string":"HTTP | id.orig_p:50451 method:GET request_body_len:0 id.resp_p:8080 uri:\/api\/v1\/clusters\/metron_cluster\/services\/KAFKA\/components\/KAFKA_BROKER?fields=metrics\/kafka\/server\/BrokerTopicMetrics\/AllTopicsBytesInPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/server\/BrokerTopicMetrics\/AllTopicsBytesOutPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/server\/BrokerTopicMetrics\/AllTopicsMessagesInPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/controller\/KafkaController\/ActiveControllerCount[1484165330,1484168930,15],metrics\/kafka\/controller\/ControllerStats\/LeaderElectionRateAndTimeMs\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/controller\/ControllerStats\/UncleanLeaderElectionsPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaFetcherManager\/Replica-MaxLag[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaManager\/PartitionCount[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaManager\/UnderReplicatedPartitions[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaManager\/LeaderCount[1484165330,1484168930,15]&format=null_padding&_=1484168930776 tags:[] uid:CUrRne3iLIxXavQtci referrer:http:\/\/node1:8080\/ trans_depth:143 host:node1 id.orig_h:192.168.66.1 response_body_len:0 user_agent:Mozilla\/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit\/537.36 (KHTML, like Gecko) Chrome\/55.0.2883.95 Safari\/537.36 ts:1517499194.7338 id.resp_h:192.168.66.121","ip_dst_addr":"192.168.66.121","threatinteljoinbolt.joiner.ts":"1517499201359","host":"node1","enrichmentjoinbolt.joiner.ts":"1517499201212","adapter.hostfromjsonlistadapter.begin.ts":"1517499201206","threatintelsplitterbolt.splitter.begin.ts":"1517499201215","ip_src_addr":"192.168.66.1","user_agent":"Mozilla\/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit\/537.36 (KHTML, like Gecko) Chrome\/55.0.2883.95 Safari\/537.36","timestamp":1517499194733,"method":"GET","request_body_len":0,"uri":"\/api\/v1\/clusters\/metron_cluster\/services\/KAFKA\/components\/KAFKA_BROKER?fields=metrics\/kafka\/server\/BrokerTopicMetrics\/AllTopicsBytesInPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/server\/BrokerTopicMetrics\/AllTopicsBytesOutPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/server\/BrokerTopicMetrics\/AllTopicsMessagesInPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/controller\/KafkaController\/ActiveControllerCount[1484165330,1484168930,15],metrics\/kafka\/controller\/ControllerStats\/LeaderElectionRateAndTimeMs\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/controller\/ControllerStats\/UncleanLeaderElectionsPerSec\/1MinuteRate[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaFetcherManager\/Replica-MaxLag[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaManager\/PartitionCount[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaManager\/UnderReplicatedPartitions[1484165330,1484168930,15],metrics\/kafka\/server\/ReplicaManager\/LeaderCount[1484165330,1484168930,15]&format=null_padding&_=1484168930776","tags":[],"source.type":"bro","adapter.geoadapter.end.ts":"1517499201209","referrer":"http:\/\/node1:8080\/","threatintelsplitterbolt.splitter.end.ts":"1517499201215","adapter.threatinteladapter.begin.ts":"1517499201217","ip_src_port":50451,"guid":"b62fe444-82fb-46a4-8c4a-5cfc248bee41","response_body_len":0}
{"adapter.threatinteladapter.end.ts":"1517499201385","bro_timestamp":"1517499194.511788","status_code":200,"ip_dst_port":80,"enrichmentsplitterbolt.splitter.end.ts":"1517499201203","enrichments.geo.ip_dst_addr.city":"Strasbourg","enrichments.geo.ip_dst_addr.latitude":"48.5839","enrichmentsplitterbolt.splitter.begin.ts":"1517499201203","adapter.hostfromjsonlistadapter.end.ts":"1517499201207","enrichments.geo.ip_dst_addr.country":"FR","enrichments.geo.ip_dst_addr.locID":"2973783","adapter.geoadapter.begin.ts":"1517499201209","enrichments.geo.ip_dst_addr.postalCode":"67100","uid":"CRGLdEasAJUDL8Tu4","resp_mime_types":["application\/x-shockwave-flash"],"trans_depth":1,"protocol":"http","original_string":"HTTP | id.orig_p:49185 status_code:200 method:GET request_body_len:0 id.resp_p:80 uri:\/ tags:[] uid:CRGLdEasAJUDL8Tu4 referrer:http:\/\/va872g.g90e1h.b8.642b63u.j985a2.v33e.37.pa269cc.e8mfzdgrf7g0.groupprograms.in\/?285a4d4e4e5a4d4d4649584c5d43064b4745 resp_mime_types:[\"application\\\/x-shockwave-flash\"] trans_depth:1 host:ubb67.3c147o.u806a4.w07d919.o5f.f1.b80w.r0faf9.e8mfzdgrf7g0.groupprograms.in status_msg:OK id.orig_h:192.168.138.158 response_body_len:8973 user_agent:Mozilla\/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident\/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0) ts:1517499194.511788 id.resp_h:62.75.195.236 resp_fuids:[\"FHMpUl2B1lUkpzZoQi\"]","ip_dst_addr":"62.75.195.236","threatinteljoinbolt.joiner.ts":"1517499201387","host":"ubb67.3c147o.u806a4.w07d919.o5f.f1.b80w.r0faf9.e8mfzdgrf7g0.groupprograms.in","enrichmentjoinbolt.joiner.ts":"1517499201213","adapter.hostfromjsonlistadapter.begin.ts":"1517499201207","threatintelsplitterbolt.splitter.begin.ts":"1517499201215","enrichments.geo.ip_dst_addr.longitude":"7.7455","ip_src_addr":"192.168.138.158","user_agent":"Mozilla\/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident\/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)","resp_fuids":["FHMpUl2B1lUkpzZoQi"],"timestamp":1517499194511,"method":"GET","request_body_len":0,"uri":"\/","tags":[],"source.type":"bro","adapter.geoadapter.end.ts":"1517499201210","referrer":"http:\/\/va872g.g90e1h.b8.642b63u.j985a2.v33e.37.pa269cc.e8mfzdgrf7g0.groupprograms.in\/?285a4d4e4e5a4d4d4649584c5d43064b4745","threatintelsplitterbolt.splitter.end.ts":"1517499201215","adapter.threatinteladapter.begin.ts":"1517499201357","ip_src_port":49185,"enrichments.geo.ip_dst_addr.location_point":"48.5839,7.7455","status_msg":"OK","guid":"04c670c2-417e-4fd5-aff6-3dd55847d3e2","response_body_len":8973}
{"adapter.threatinteladapter.end.ts":"1517499201399","bro_timestamp":"1517499194.20478","status_code":404,"ip_dst_port":80,"enrichmentsplitterbolt.splitter.end.ts":"1517499201203","enrichments.geo.ip_dst_addr.city":"Phoenix","enrichments.geo.ip_dst_addr.latitude":"33.4499","enrichmentsplitterbolt.splitter.begin.ts":"1517499201203","adapter.hostfromjsonlistadapter.end.ts":"1517499201207","enrichments.geo.ip_dst_addr.country":"US","enrichments.geo.ip_dst_addr.locID":"5308655","adapter.geoadapter.begin.ts":"1517499201210","enrichments.geo.ip_dst_addr.postalCode":"85004","uid":"CgI9Lp32cTchxqp8Wk","resp_mime_types":["text\/html"],"trans_depth":1,"protocol":"http","original_string":"HTTP | id.orig_p:49199 status_code:404 method:POST request_body_len:96 id.resp_p:80 orig_mime_types:[\"text\\\/plain\"] uri:\/wp-content\/themes\/twentyfifteen\/img5.php?l=8r1gf1b2t1kuq42 tags:[] uid:CgI9Lp32cTchxqp8Wk resp_mime_types:[\"text\\\/html\"] trans_depth:1 orig_fuids:[\"FDpZNy3tiCh1cjvs19\"] host:runlove.us status_msg:Not Found id.orig_h:192.168.138.158 response_body_len:357 user_agent:Mozilla\/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident\/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0) ts:1517499194.20478 id.resp_h:204.152.254.221 resp_fuids:[\"FCCDfF1umBiOBkbAl3\"]","ip_dst_addr":"204.152.254.221","threatinteljoinbolt.joiner.ts":"1517499201401","enrichments.geo.ip_dst_addr.dmaCode":"753","host":"runlove.us","enrichmentjoinbolt.joiner.ts":"1517499201273","adapter.hostfromjsonlistadapter.begin.ts":"1517499201207","threatintelsplitterbolt.splitter.begin.ts":"1517499201276","enrichments.geo.ip_dst_addr.longitude":"-112.0712","ip_src_addr":"192.168.138.158","user_agent":"Mozilla\/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident\/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)","resp_fuids":["FCCDfF1umBiOBkbAl3"],"timestamp":1517499194204,"method":"POST","request_body_len":96,"orig_mime_types":["text\/plain"],"uri":"\/wp-content\/themes\/twentyfifteen\/img5.php?l=8r1gf1b2t1kuq42","tags":[],"source.type":"bro","adapter.geoadapter.end.ts":"1517499201270","threatintelsplitterbolt.splitter.end.ts":"1517499201276","adapter.threatinteladapter.begin.ts":"1517499201385","orig_fuids":["FDpZNy3tiCh1cjvs19"],"ip_src_port":49199,"enrichments.geo.ip_dst_addr.location_point":"33.4499,-112.0712","status_msg":"Not Found","guid":"e78f4fbd-1728-4f5d-814a-588998653cc5","response_body_len":357}
{"adapter.threatinteladapter.end.ts":"1517499201399","bro_timestamp":"1517499194.548579","status_code":200,"ip_dst_port":80,"enrichmentsplitterbolt.splitter.end.ts":"1517499201203","enrichments.geo.ip_dst_addr.city":"Strasbourg","enrichments.geo.ip_dst_addr.latitude":"48.5839","enrichmentsplitterbolt.splitter.begin.ts":"1517499201203","adapter.hostfromjsonlistadapter.end.ts":"1517499201207","enrichments.geo.ip_dst_addr.country":"FR","enrichments.geo.ip_dst_addr.locID":"2973783","adapter.geoadapter.begin.ts":"1517499201270","enrichments.geo.ip_dst_addr.postalCode":"67100","uid":"CMoJLQHEghS3LbRW5","trans_depth":1,"protocol":"http","original_string":"HTTP | id.orig_p:49190 status_code:200 method:GET request_body_len:0 id.resp_p:80 uri:\/?b2566564b3ba1a38e61c83957a7dbcd5 tags:[] uid:CMoJLQHEghS3LbRW5 trans_depth:1 host:62.75.195.236 status_msg:OK id.orig_h:192.168.138.158 response_body_len:0 user_agent:Mozilla\/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident\/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0) ts:1517499194.548579 id.resp_h:62.75.195.236","ip_dst_addr":"62.75.195.236","threatinteljoinbolt.joiner.ts":"1517499201401","host":"62.75.195.236","enrichmentjoinbolt.joiner.ts":"1517499201273","adapter.hostfromjsonlistadapter.begin.ts":"1517499201207","threatintelsplitterbolt.splitter.begin.ts":"1517499201276","enrichments.geo.ip_dst_addr.longitude":"7.7455","ip_src_addr":"192.168.138.158","user_agent":"Mozilla\/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident\/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)","timestamp":1517499194548,"method":"GET","request_body_len":0,"uri":"\/?b2566564b3ba1a38e61c83957a7dbcd5","tags":[],"source.type":"bro","adapter.geoadapter.end.ts":"1517499201270","threatintelsplitterbolt.splitter.end.ts":"1517499201276","adapter.threatinteladapter.begin.ts":"1517499201399","ip_src_port":49190,"enrichments.geo.ip_dst_addr.location_point":"48.5839,7.7455","status_msg":"OK","guid":"8fbfb4df-07f4-48cf-aa0b-6dd491d765d4","response_body_len":0}
{"adapter.threatinteladapter.end.ts":"1517499201456","qclass_name":"qclass-32769","bro_timestamp":"1517499194.746276","qtype_name":"PTR","ip_dst_port":5353,"enrichmentsplitterbolt.splitter.end.ts":"1517499201204","qtype":12,"rejected":false,"enrichmentsplitterbolt.splitter.begin.ts":"1517499201204","adapter.hostfromjsonlistadapter.end.ts":"1517499201207","trans_id":0,"adapter.geoadapter.begin.ts":"1517499201270","uid":"Cqfoel1A3zgfxBLO58","protocol":"dns","original_string":"DNS | AA:false qclass_name:qclass-32769 id.orig_p:5353 qtype_name:PTR qtype:12 rejected:false id.resp_p:5353 query:_googlecast._tcp.local trans_id:0 TC:false RA:false uid:Cqfoel1A3zgfxBLO58 RD:false proto:udp id.orig_h:192.168.66.1 Z:0 qclass:32769 ts:1517499194.746276 id.resp_h:224.0.0.251","ip_dst_addr":"224.0.0.251","threatinteljoinbolt.joiner.ts":"1517499201459","enrichmentjoinbolt.joiner.ts":"1517499201274","adapter.hostfromjsonlistadapter.begin.ts":"1517499201207","threatintelsplitterbolt.splitter.begin.ts":"1517499201276","Z":0,"ip_src_addr":"192.168.66.1","qclass":32769,"timestamp":1517499194746,"AA":false,"query":"_googlecast._tcp.local","TC":false,"RA":false,"source.type":"bro","adapter.geoadapter.end.ts":"1517499201270","RD":false,"threatintelsplitterbolt.splitter.end.ts":"1517499201276","adapter.threatinteladapter.begin.ts":"1517499201399","ip_src_port":5353,"proto":"udp","guid":"77f3743d-b931-4022-bdbb-cf22e1d45af3"}
   */
  @Multiline
  public static String broData;

  /**
{"msg":"'snort test alert'","adapter.threatinteladapter.end.ts":"1517499195495","sig_rev":"0","ip_dst_port":"50183","enrichmentsplitterbolt.splitter.end.ts":"1517499192333","ethsrc":"08:00:27:E8:B0:7A","threat.triage.rules.0.comment":null,"tcpseq":"0x8DF4FA2F","threat.triage.score":10.0,"dgmlen":"52","enrichmentsplitterbolt.splitter.begin.ts":"1517499192195","adapter.hostfromjsonlistadapter.end.ts":"1517499192400","adapter.geoadapter.begin.ts":"1517499192446","tcpwindow":"0x1F5","threat.triage.rules.0.score":10,"tcpack":"0x8368306E","protocol":"TCP","ip_dst_addr":"192.168.66.1","original_string":"02\/01\/18-15:33:07.000000 ,1,999158,0,\"'snort test alert'\",TCP,192.168.66.121,8080,192.168.66.1,50183,08:00:27:E8:B0:7A,0A:00:27:00:00:00,0x42,***A****,0x8DF4FA2F,0x8368306E,,0x1F5,64,0,62260,52,53248,,,,","threatinteljoinbolt.joiner.ts":"1517499195528","enrichmentjoinbolt.joiner.ts":"1517499192965","threat.triage.rules.0.reason":null,"tos":"0","adapter.hostfromjsonlistadapter.begin.ts":"1517499192400","threatintelsplitterbolt.splitter.begin.ts":"1517499193330","id":"62260","ip_src_addr":"192.168.66.121","timestamp":1517499187000,"ethdst":"0A:00:27:00:00:00","threat.triage.rules.0.name":null,"is_alert":"true","ttl":"64","source.type":"snort","adapter.geoadapter.end.ts":"1517499192723","ethlen":"0x42","iplen":"53248","threatintelsplitterbolt.splitter.end.ts":"1517499193359","adapter.threatinteladapter.begin.ts":"1517499193366","ip_src_port":"8080","tcpflags":"***A****","guid":"b486ac73-6c5f-425c-92c3-5f2542b53c35","sig_id":"999158","sig_generator":"1"}
{"msg":"'snort test alert'","adapter.threatinteladapter.end.ts":"1517499195797","enrichmentsplitterbolt.splitter.end.ts":"1517499192359","enrichments.geo.ip_dst_addr.city":"Strasbourg","threat.triage.rules.0.comment":null,"dgmlen":"353","enrichments.geo.ip_dst_addr.country":"FR","enrichments.geo.ip_dst_addr.locID":"2973783","tcpack":"0xB640F4","protocol":"TCP","original_string":"02\/01\/18-15:33:07.000000 ,1,999158,0,\"'snort test alert'\",TCP,192.168.138.158,49192,62.75.195.236,80,00:00:00:00:00:00,00:00:00:00:00:00,0x16F,***AP***,0xD57E2000,0xB640F4,,0xFAF0,128,0,2416,353,99332,,,,","enrichmentjoinbolt.joiner.ts":"1517499193236","adapter.hostfromjsonlistadapter.begin.ts":"1517499192452","id":"2416","adapter.geoadapter.end.ts":"1517499193234","ethlen":"0x16F","adapter.threatinteladapter.begin.ts":"1517499195496","enrichments.geo.ip_dst_addr.location_point":"48.5839,7.7455","tcpflags":"***AP***","guid":"27a11b7a-9ed2-4a49-b177-04acc30b69c5","sig_rev":"0","ip_dst_port":"80","ethsrc":"00:00:00:00:00:00","enrichments.geo.ip_dst_addr.latitude":"48.5839","tcpseq":"0xD57E2000","threat.triage.score":10.0,"enrichmentsplitterbolt.splitter.begin.ts":"1517499192359","adapter.hostfromjsonlistadapter.end.ts":"1517499192452","adapter.geoadapter.begin.ts":"1517499192723","tcpwindow":"0xFAF0","enrichments.geo.ip_dst_addr.postalCode":"67100","threat.triage.rules.0.score":10,"ip_dst_addr":"62.75.195.236","threatinteljoinbolt.joiner.ts":"1517499195801","threat.triage.rules.0.reason":null,"tos":"0","threatintelsplitterbolt.splitter.begin.ts":"1517499193359","enrichments.geo.ip_dst_addr.longitude":"7.7455","ip_src_addr":"192.168.138.158","timestamp":1517499187000,"ethdst":"00:00:00:00:00:00","threat.triage.rules.0.name":null,"is_alert":"true","ttl":"128","source.type":"snort","iplen":"99332","threatintelsplitterbolt.splitter.end.ts":"1517499193359","ip_src_port":"49192","sig_id":"999158","sig_generator":"1"}
{"msg":"'snort test alert'","adapter.threatinteladapter.end.ts":"1517499196016","sig_rev":"0","ip_dst_port":"8080","enrichmentsplitterbolt.splitter.end.ts":"1517499192360","ethsrc":"0A:00:27:00:00:00","threat.triage.rules.0.comment":null,"tcpseq":"0xE6B38B18","threat.triage.score":10.0,"dgmlen":"52","enrichmentsplitterbolt.splitter.begin.ts":"1517499192360","adapter.hostfromjsonlistadapter.end.ts":"1517499192452","adapter.geoadapter.begin.ts":"1517499193234","tcpwindow":"0xFF2","threat.triage.rules.0.score":10,"tcpack":"0x79C2FA21","protocol":"TCP","ip_dst_addr":"192.168.66.121","original_string":"02\/01\/18-15:33:07.000000 ,1,999158,0,\"'snort test alert'\",TCP,192.168.66.1,50186,192.168.66.121,8080,0A:00:27:00:00:00,08:00:27:E8:B0:7A,0x42,***A****,0xE6B38B18,0x79C2FA21,,0xFF2,64,0,31478,52,53248,,,,","threatinteljoinbolt.joiner.ts":"1517499196019","enrichmentjoinbolt.joiner.ts":"1517499193238","threat.triage.rules.0.reason":null,"tos":"0","adapter.hostfromjsonlistadapter.begin.ts":"1517499192452","threatintelsplitterbolt.splitter.begin.ts":"1517499193359","id":"31478","ip_src_addr":"192.168.66.1","timestamp":1517499187000,"ethdst":"08:00:27:E8:B0:7A","threat.triage.rules.0.name":null,"is_alert":"true","ttl":"64","source.type":"snort","adapter.geoadapter.end.ts":"1517499193236","ethlen":"0x42","iplen":"53248","threatintelsplitterbolt.splitter.end.ts":"1517499193360","adapter.threatinteladapter.begin.ts":"1517499195797","ip_src_port":"50186","tcpflags":"***A****","guid":"50f8de4d-d3ef-4f31-b337-5ea67493ebe5","sig_id":"999158","sig_generator":"1"}
{"msg":"'snort test alert'","adapter.threatinteladapter.end.ts":"1517499196016","enrichmentsplitterbolt.splitter.end.ts":"1517499192400","enrichments.geo.ip_dst_addr.city":"Strasbourg","threat.triage.rules.0.comment":null,"dgmlen":"40","enrichments.geo.ip_dst_addr.country":"FR","enrichments.geo.ip_dst_addr.locID":"2973783","tcpack":"0x7371702D","protocol":"TCP","original_string":"02\/01\/18-15:33:07.000000 ,1,999158,0,\"'snort test alert'\",TCP,192.168.138.158,49186,62.75.195.236,80,00:00:00:00:00:00,00:00:00:00:00:00,0x3C,***A****,0x516C475D,0x7371702D,,0xFAF0,128,0,2257,40,40960,,,,","enrichmentjoinbolt.joiner.ts":"1517499193239","adapter.hostfromjsonlistadapter.begin.ts":"1517499192452","id":"2257","adapter.geoadapter.end.ts":"1517499193236","ethlen":"0x3C","adapter.threatinteladapter.begin.ts":"1517499196016","enrichments.geo.ip_dst_addr.location_point":"48.5839,7.7455","tcpflags":"***A****","guid":"054ff2bb-4d29-4cfc-b225-fef7488b96a6","sig_rev":"0","ip_dst_port":"80","ethsrc":"00:00:00:00:00:00","enrichments.geo.ip_dst_addr.latitude":"48.5839","tcpseq":"0x516C475D","threat.triage.score":10.0,"enrichmentsplitterbolt.splitter.begin.ts":"1517499192369","adapter.hostfromjsonlistadapter.end.ts":"1517499192452","adapter.geoadapter.begin.ts":"1517499193236","tcpwindow":"0xFAF0","enrichments.geo.ip_dst_addr.postalCode":"67100","threat.triage.rules.0.score":10,"ip_dst_addr":"62.75.195.236","threatinteljoinbolt.joiner.ts":"1517499196020","threat.triage.rules.0.reason":null,"tos":"0","threatintelsplitterbolt.splitter.begin.ts":"1517499193360","enrichments.geo.ip_dst_addr.longitude":"7.7455","ip_src_addr":"192.168.138.158","timestamp":1517499187000,"ethdst":"00:00:00:00:00:00","threat.triage.rules.0.name":null,"is_alert":"true","ttl":"128","source.type":"snort","iplen":"40960","threatintelsplitterbolt.splitter.end.ts":"1517499193360","ip_src_port":"49186","sig_id":"999158","sig_generator":"1"}
{"msg":"'snort test alert'","adapter.threatinteladapter.end.ts":"1517499196062","enrichments.geo.ip_src_addr.longitude":"7.7455","enrichmentsplitterbolt.splitter.end.ts":"1517499192448","threat.triage.rules.0.comment":null,"dgmlen":"1407","enrichments.geo.ip_src_addr.city":"Strasbourg","tcpack":"0x9DFB1927","protocol":"TCP","original_string":"02\/01\/18-15:33:07.000000 ,1,999158,0,\"'snort test alert'\",TCP,62.75.195.236,80,192.168.138.158,49189,00:00:00:00:00:00,00:00:00:00:00:00,0x58D,***AP***,0xF1BC1268,0x9DFB1927,,0xFAF0,128,0,1722,1407,130068,,,,","enrichmentjoinbolt.joiner.ts":"1517499193239","adapter.hostfromjsonlistadapter.begin.ts":"1517499192452","id":"1722","adapter.geoadapter.end.ts":"1517499193238","ethlen":"0x58D","adapter.threatinteladapter.begin.ts":"1517499196016","tcpflags":"***AP***","guid":"65366689-c232-46bf-a3ae-ad72ab560a70","sig_rev":"0","ip_dst_port":"49189","enrichments.geo.ip_src_addr.location_point":"48.5839,7.7455","ethsrc":"00:00:00:00:00:00","tcpseq":"0xF1BC1268","threat.triage.score":10.0,"enrichmentsplitterbolt.splitter.begin.ts":"1517499192448","adapter.hostfromjsonlistadapter.end.ts":"1517499192452","adapter.geoadapter.begin.ts":"1517499193236","tcpwindow":"0xFAF0","enrichments.geo.ip_src_addr.postalCode":"67100","threat.triage.rules.0.score":10,"ip_dst_addr":"192.168.138.158","enrichments.geo.ip_src_addr.latitude":"48.5839","threatinteljoinbolt.joiner.ts":"1517499196065","threat.triage.rules.0.reason":null,"tos":"0","threatintelsplitterbolt.splitter.begin.ts":"1517499193360","enrichments.geo.ip_src_addr.locID":"2973783","ip_src_addr":"62.75.195.236","enrichments.geo.ip_src_addr.country":"FR","timestamp":1517499187000,"ethdst":"00:00:00:00:00:00","threat.triage.rules.0.name":null,"is_alert":"true","ttl":"128","source.type":"snort","iplen":"130068","threatintelsplitterbolt.splitter.end.ts":"1517499193360","ip_src_port":"80","sig_id":"999158","sig_generator":"1"}
   */
  @Multiline
  public static String snortData;


  public static Map<String, Object> getGlobalConfig(String sensorType, SolrComponent component) {
    Map<String, Object> globalConfig = new HashMap<>();
    globalConfig.put("solr.zookeeper", component.getZookeeperUrl());
    globalConfig.put("solr.collection", sensorType + "_doc");
    globalConfig.put("solr.numShards", 1);
    globalConfig.put("solr.replicationFactor", 1);
    return globalConfig;
  }

  public static SolrComponent createSolrComponent(String sensor) throws Exception {
    return new SolrComponent.Builder()
            .addCollection(String.format("%s_doc", sensor), String.format("src/main/config/schema/%s", sensor))
            .build();
  }

  @Test
  public void testBro() throws Exception {
    test("bro", broData);
  }

  @Test
  public void testSnort() throws Exception {
    test("snort", snortData);
  }

  public void test(String sensorType, String data) throws Exception {
    SolrComponent component = null;
    try {
      component = createSolrComponent(sensorType);
      component.start();
      Map<String, Object> globalConfig = getGlobalConfig(sensorType, component);

      List<JSONObject> inputs = new ArrayList<>();
      Map<String, Map<String, Object>> index = new HashMap<>();
      for (String message : Splitter.on("\n").split(data)) {
        if (message.trim().length() > 0) {
          Map<String, Object> m = JSONUtils.INSTANCE.load(message.trim(), JSONUtils.MAP_SUPPLIER);
          Assert.assertTrue(m.containsKey("guid"));
          index.put("" + m.get("guid"), m);
          inputs.add(new JSONObject(m));
        }
      }

      SolrWriter solrWriter = new SolrWriter() {
        @Override
        protected String getFieldName(Object key, Object value) {
          return "" + key;
        }

        @Override
        protected Object getIdValue(JSONObject message) {
          return message.get("guid");
        }

      };
      WriterConfiguration writerConfig = new WriterConfiguration() {
        @Override
        public int getBatchSize(String sensorName) {
          return inputs.size();
        }

        @Override
        public int getBatchTimeout(String sensorName) {
          return 0;
        }

        @Override
        public List<Integer> getAllConfiguredTimeouts() {
          return new ArrayList<>();
        }

        @Override
        public String getIndex(String sensorName) {
          return sensorType;
        }

        @Override
        public boolean isEnabled(String sensorName) {
          return true;
        }

        @Override
        public Map<String, Object> getSensorConfig(String sensorName) {
          return new HashMap<String, Object>() {{
            put("index", sensorType);
            put("batchSize", inputs.size());
            put("enabled", true);
          }};
        }

        @Override
        public Map<String, Object> getGlobalConfig() {
          return globalConfig;
        }

        @Override
        public boolean isDefault(String sensorName) {
          return false;
        }
      };

      solrWriter.init(null, null, writerConfig);

      solrWriter.write(sensorType, writerConfig, new ArrayList<>(), inputs);
      for (Map<String, Object> m : component.getAllIndexedDocs(sensorType + "_doc")) {
        Map<String, Object> expected = index.get("" + m.get("guid"));
        for (Map.Entry<String, Object> field : expected.entrySet()) {
          if (field.getValue() instanceof Collection && ((Collection) field.getValue()).size() == 0) {
            continue;
          }
          if(field.getValue() instanceof Number) {
            Number n1 = (Number)field.getValue();
            Number n2 = (Number)m.get(field.getKey());
            Assert.assertEquals("Unable to find " + field.getKey(), n1.doubleValue(), n2.doubleValue(), 1e-6);
          }
          else {
            Assert.assertEquals("Unable to find " + field.getKey(), "" + field.getValue(), "" + m.get(field.getKey()));
          }
        }
      }
    }
    finally {
      if(component != null) {
        component.stop();
      }
    }
  }

}
