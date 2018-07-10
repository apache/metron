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
package org.apache.metron.rest.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.utils.HDFSUtils;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.Statusable;
import org.apache.metron.job.manager.JobManager;
import org.apache.metron.job.service.JobServiceStrategies;
import org.apache.metron.job.writer.PrefixStrategy;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.pcap.mr.PcapMRJobConfig;
import org.apache.metron.pcap.writer.PcapResultsWriter;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.PcapResponse;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.apache.metron.rest.service.PcapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class PcapServiceImpl implements PcapService {

  private Environment environment;
  private Configuration configuration;
  private JobManager<Path> jobManager;

  @Autowired
  public PcapServiceImpl(Environment environment, Configuration configuration) {
    this.environment = environment;
    this.configuration = configuration;
    this.jobManager = new JobManager<>(JobServiceStrategies.HDFS);
  }

  @Override
  public JobStatus fixed(FixedPcapRequest fixedPcapRequest) throws RestException {
    if (fixedPcapRequest.getBasePath() == null) {
      fixedPcapRequest.setBasePath(environment.getProperty(MetronRestConstants.PCAP_INPUT_PATH_SPRING_PROPERTY));
    }
    if (fixedPcapRequest.getBaseOutputPath() == null) {
      fixedPcapRequest.setBaseOutputPath(environment.getProperty(MetronRestConstants.PCAP_OUTPUT_PATH_SPRING_PROPERTY));
    }

    PrefixStrategy prefixStrategy = clock -> {
      String timestamp = new Clock().currentTimeFormatted("yyyyMMddHHmm");
      String uuid = UUID.randomUUID().toString().replaceAll("-", "");
      return String.format("%s-%s", timestamp, uuid);
    };

    PcapMRJobConfig<Map<String, String>> config = new PcapMRJobConfig();
    try {
      config.setBasePath(new Path(fixedPcapRequest.getBasePath()))
          .setBaseOutputPath(new Path(fixedPcapRequest.getBaseOutputPath()))
          .setBeginNS(TimestampConverters.MILLISECONDS.toNanoseconds(fixedPcapRequest.getStartTime()))
          .setEndNS(TimestampConverters.MILLISECONDS.toNanoseconds(fixedPcapRequest.getEndTime()))
          .setNumReducers(fixedPcapRequest.getNumReducers())
          .setFields(getFixedFields(fixedPcapRequest))
          .setConf(configuration)
          .setFs(getFileSystem())
          .setFilterImpl(new FixedPcapFilter.Configurator())
          .setResultsWriter(new PcapResultsWriter())
          .setFinalOutputPath(new Path("/apps/metron/pcap/final"))
          .setNumRecordsPerFile(10)
          .setOutputFilePrefix(prefixStrategy.apply(new Clock()));

      Statusable<Path> pcapJob = new PcapJob(config);
      return jobManager.submit(pcapJob, new HashMap<>(), "metron");
    } catch (IOException | JobException e) {
      throw new RestException(e);
    }
  }

  @Override
  public JobStatus getJobStatus(String username, String jobId) throws RestException {
    try {
      return jobManager.getStatus(username, jobId);
    } catch (JobException e) {
      throw new RestException("Unable to get job status", e);
    }
  }

  @Override
  public PcapResponse getPage(String username, String jobId, int pageNum) throws RestException {
    PcapResponse response = new PcapResponse();
    Statusable<Path> job = jobManager.getJob(username, jobId);
    try {
      if (job.isDone()) {
        Pageable<Path> results = job.getFinalResults();
        Path resultsPage = results.getPage(pageNum);
        if (resultsPage != null) {
          List<byte[]> pcaps = new ArrayList<>();
          byte[] pcap = HDFSUtils.readBytes(resultsPage.toString());
          pcaps.add(pcap);
          response.setPcaps(pcaps);
        }
      }
    } catch (JobException | IOException e) {
      throw new RestException("Unable to get results", e);
    }
    return response;
  }

  protected Map<String, String> getFixedFields(FixedPcapRequest fixedPcapRequest) {
    Map<String, String> fixedFields = new HashMap<>();
    if (fixedPcapRequest.getIpSrcAddr() != null) {
      fixedFields.put(Constants.Fields.SRC_ADDR.getName(), fixedPcapRequest.getIpSrcAddr());
    }
    if (fixedPcapRequest.getIpDstAddr() != null) {
      fixedFields.put(Constants.Fields.DST_ADDR.getName(), fixedPcapRequest.getIpDstAddr());
    }
    if (fixedPcapRequest.getIpSrcPort() != null) {
      fixedFields.put(Constants.Fields.SRC_PORT.getName(), fixedPcapRequest.getIpSrcPort().toString());
    }
    if (fixedPcapRequest.getIpDstPort() != null) {
      fixedFields.put(Constants.Fields.DST_PORT.getName(), fixedPcapRequest.getIpDstPort().toString());
    }
    if (fixedPcapRequest.getProtocol() != null) {
      fixedFields.put(Constants.Fields.PROTOCOL.getName(), fixedPcapRequest.getProtocol());
    }
    if (fixedPcapRequest.getIncludeReverse() != null) {
      fixedFields.put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), fixedPcapRequest.getIncludeReverse().toString());
    }
    if (fixedPcapRequest.getPacketFilter() != null) {
      fixedFields.put(PcapHelper.PacketFields.PACKET_FILTER.getName(), fixedPcapRequest.getPacketFilter());
    }
    return fixedFields;
  }

  protected FileSystem getFileSystem() throws IOException {
    return FileSystem.get(configuration);
  }
}
