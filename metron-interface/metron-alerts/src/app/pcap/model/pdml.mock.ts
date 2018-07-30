/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing software
 * distributed under the License is distributed on an "AS IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { PdmlPacket, PdmlProto, PdmlField } from './pdml';

export const fakePacket = {
  "name": '',
  "expanded": false,
  "protos": [
    {
      "name": "geninfo",
      "showname": "",
      "fields": [
        { "name": "timestamp", "pos": "0", "showname": "Captured Time", "size": "722", "value": "1458240269.373968000", "show": "Mar 17, 2016 18:44:29.373968000 UTC", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField
      ]
    } as PdmlProto,
    {
      "name": "ip",
      "showname": "",
      "fields": [
        { "name": "ip.proto", "pos": "23", "showname": "Protocol: TCP (6)", "size": "1", "value": "06", "show": "6", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField,
        { "name": "ip.src", "pos": "26", "showname": "Source: 192.168.66.121 (192.168.66.121)", "size": "4", "value": "c0a84279", "show": "192.168.66.121", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField,
        { "name": "ip.dst", "pos": "30", "showname": "Destination: 192.168.66.1 (192.168.66.1)", "size": "4", "value": "c0a84201", "show": "192.168.66.1", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField
      ]
    } as PdmlProto,
    {
      "name": "tcp",
      "showname": "",
      "fields": [
        { "name": "tcp.srcport", "pos": "34", "showname": "Source port: ssh (22)", "size": "2", "value": "0016", "show": "22", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField,
        { "name": "tcp.dstport", "pos": "36", "showname": "Destination port: 55791 (55791)", "size": "2", "value": "d9ef", "show": "55791", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField
      ],
    } as PdmlProto
  ]
} as PdmlPacket;