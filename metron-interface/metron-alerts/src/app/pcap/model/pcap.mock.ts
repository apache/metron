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
import { PcapRequest } from './pcap.request';
import { PcapStatusResponse } from '../model/pcap-status-response';

export const fakePcapRequest = {
  startTimeMs: 0,
  endTimeMs: 0,
  ipSrcAddr: '0.0.0.0',
  ipSrcPort: '80',
  ipDstAddr: '0.0.0.0',
  ipDstPort: '80',
  protocol: '*',
  packetFilter: '*',
  includeReverse: false
} as PcapRequest;

export const fakePcapStatusResponse = {
  jobId: 'job_1234567890123_4567',
  jobStatus: 'SUBMITTED',
  description: 'Job submitted.',
  percentComplete: 0.0,
  pageTotal: 0
} as PcapStatusResponse;
