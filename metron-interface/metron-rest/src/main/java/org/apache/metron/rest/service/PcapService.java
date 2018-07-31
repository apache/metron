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
import java.util.Map;

public interface PcapService {

  /**
   * Submits a pcap query.  Jobs that do not match any pcap data will return a status without a job id and a
   * description of why data did not match.
   *
   * @param username User that is submitting the job
   * @param pcapRequest Either a fixed or query filter pcap request
   * @return Status of the submitted job
   * @throws RestException
   */
  PcapStatus submit(String username, PcapRequest pcapRequest) throws RestException;

  /**
   * Returns the status of a job given a job id and user who submitted it.
   *
   * @param username User that submitted the job
   * @param jobId Job id of a submitted job
   * @return Status of the submitted job or null if job does not exist
   * @throws RestException
   */
  PcapStatus getJobStatus(String username, String jobId) throws RestException;

  /**
   * Returns a list of statuses for jobs a user has submitted that match the given job state.
   *
   * @param username User that submitted the job
   * @param state Job state
   * @return List of job statuses of an empty list of no jobs match
   * @throws RestException
   */
  List<PcapStatus> getJobStatus(String username, JobStatus.State state) throws RestException;

  /**
   * Kills a running job.
   *
   * @param username User that submitted the job
   * @param jobId Job id of a submitted job
   * @return Status of the killed job
   * @throws RestException
   */
  PcapStatus killJob(String username, String jobId) throws RestException;

  /**
   * Gets an HDFS path to results of a pcap query given a user, job id and page number.
   *
   * @param username User that submitted the job
   * @param jobId Job id
   * @param page Page number
   * @return HDFS path to results
   * @throws RestException
   */
  Path getPath(String username, String jobId, Integer page) throws RestException;

  /**
   * Gets pcap query results in <a href="https://wiki.wireshark.org/PDML">PDML</a> format for a given user, job id and page number.
   *
   * @param username User that submitted the job
   * @param jobId Job id
   * @param page Page number
   * @return Results in PDML format
   * @throws RestException
   */
  Pdml getPdml(String username, String jobId, Integer page) throws RestException;

  /**
   * Returns an input stream of raw binary pcap results for a given user, job id and page number.
   *
   * @param username User that submitted the job
   * @param jobId Job id
   * @param page Page number
   * @return Input stream of raw pcap results
   * @throws RestException
   */
  InputStream getRawPcap(String username, String jobId, Integer page) throws RestException;

  /**
   * Gets the configuration of a submitted pcap query.  Internal properties such as file system, hadoop config, etc are not included.
   *
   * @param username User that submitted the job
   * @param jobId Job id
   * @return Configuration of a submitted pcap query
   * @throws RestException
   */
  Map<String, Object> getConfiguration(String username, String jobId) throws RestException;
}
