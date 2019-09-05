/*
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
package org.apache.metron.rest.mock;

import java.nio.charset.StandardCharsets;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.service.impl.PcapToPdmlScriptWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MockPcapToPdmlScriptWrapper extends PcapToPdmlScriptWrapper {

  /**
   *<?xml version="1.0" encoding="utf-8"?>
   *<?xml-stylesheet type="text/xsl" href="pdml2html.xsl"?>
   *<pdml version="0" creator="wireshark/2.6.1" time="Thu Jun 28 14:14:38 2018" capture_file="/tmp/pcap-data-201806272004-289365c53112438ca55ea047e13a12a5+0001.pcap">
   *<packet>
   *<proto name="geninfo" pos="0" showname="General information" size="722" hide="no">
   *<field name="num" pos="0" show="1" showname="Number" value="1" size="722"/>
   *</proto>
   *<proto name="ip" showname="Internet Protocol Version 4, Src: 192.168.66.1, Dst: 192.168.66.121" size="20" pos="14" hide="yes">
   *<field name="ip.addr" showname="Source or Destination Address: 192.168.66.121" hide="yes" size="4" pos="30" show="192.168.66.121" value="c0a84279"/>
   *<field name="ip.flags" showname="Flags: 0x4000, Don&#x27;t fragment" size="2" pos="20" show="0x00004000" value="4000">
   *<field name="ip.flags.mf" showname="..0. .... .... .... = More fragments: Not set" size="2" pos="20" show="0" value="0" unmaskedvalue="4000"/>
   *</field>
   *</proto>
   *</packet>
   *</pdml>
   */
  @Multiline
  private String pdmlXml;

  @Override
  public InputStream execute(String scriptPath, FileSystem fileSystem, Path pcapPath) throws IOException {
    return new ByteArrayInputStream(pdmlXml.getBytes(StandardCharsets.UTF_8));
  }
}
