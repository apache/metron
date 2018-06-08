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
import {AlertComment} from '../alerts/alert-details/alert-comment';

export class AlertSource {
  msg: string;
  sig_rev: number;
  ip_dst_port: number;
  ethsrc: string;
  tcpseq: number;
  dgmlen: number;
  tcpwindow: number;
  tcpack: number;
  protocol: string;
  'source:type': string;
  ip_dst_addr: number;
  original_string: string;
  tos: number;
  id: number;
  ip_src_addr: string;
  timestamp: number;
  ethdst: string;
  is_alert: boolean;
  ttl: number;
  ethlen: number;
  iplen: number;
  ip_src_port: number;
  tcpflags: string;
  guid: string;
  sig_id: number;
  sig_generator: number;
  metron_alert: AlertSource[] = [];
  comments: AlertComment[] = [];
  'threat:triage:score': number;
  'threatinteljoinbolt:joiner:ts': number;
  'enrichmentsplitterbolt:splitter:begin:ts': number;
  'enrichmentjoinbolt:joiner:ts': number;
  'threatintelsplitterbolt:splitter:end:ts': number;
  'enrichmentsplitterbolt:splitter:end:ts': number;
  'threatintelsplitterbolt:splitter:begin:ts': number;
}
