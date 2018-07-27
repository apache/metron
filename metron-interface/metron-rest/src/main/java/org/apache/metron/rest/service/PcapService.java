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
package org.apache.metron.rest.service;

import org.apache.hadoop.fs.Path;
import org.apache.metron.job.JobStatus;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.pcap.PcapRequest;
import org.apache.metron.rest.model.pcap.PcapStatus;
import org.apache.metron.rest.model.pcap.Pdml;

import java.io.InputStream;
import java.util.List;

public interface PcapService {

  PcapStatus submit(String username, PcapRequest pcapRequest) throws RestException;

  PcapStatus getJobStatus(String username, String jobId) throws RestException;

  List<PcapStatus> getJobStatus(String username, JobStatus.State state) throws RestException;

  PcapStatus killJob(String username, String jobId) throws RestException;

  Path getPath(String username, String jobId, Integer page) throws RestException;

  Pdml getPdml(String username, String jobId, Integer page) throws RestException;

  InputStream getRawPcap(String username, String jobId, Integer page) throws RestException;


}
