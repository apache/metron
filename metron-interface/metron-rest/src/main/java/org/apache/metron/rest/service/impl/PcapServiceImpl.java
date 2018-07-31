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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobNotFoundException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.Statusable;
import org.apache.metron.job.manager.JobManager;
import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.config.PcapJobSupplier;
import org.apache.metron.rest.model.pcap.FixedPcapOptions;
import org.apache.metron.rest.model.pcap.PcapRequest;
import org.apache.metron.rest.model.pcap.PcapStatus;
import org.apache.metron.rest.model.pcap.Pdml;
import org.apache.metron.rest.model.pcap.QueryPcapOptions;
import org.apache.metron.rest.service.PcapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PcapServiceImpl implements PcapService {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Environment environment;
  private Configuration configuration;
  private PcapJobSupplier pcapJobSupplier;
  private JobManager<Path> jobManager;
  private PcapToPdmlScriptWrapper pcapToPdmlScriptWrapper;

  @Autowired
  public PcapServiceImpl(Environment environment, Configuration configuration, PcapJobSupplier pcapJobSupplier, JobManager<Path> jobManager, PcapToPdmlScriptWrapper pcapToPdmlScriptWrapper) {
    this.environment = environment;
    this.configuration = configuration;
    this.pcapJobSupplier = pcapJobSupplier;
    this.jobManager = jobManager;
    this.pcapToPdmlScriptWrapper = pcapToPdmlScriptWrapper;
  }

  @Override
  public PcapStatus submit(String username, PcapRequest pcapRequest) throws RestException {
    List<PcapStatus> runningJobs = getJobStatus(username, JobStatus.State.RUNNING);
    Integer userJobLimit = environment.getProperty(MetronRestConstants.USER_JOB_LIMIT_SPRING_PROPERTY, Integer.class, 1);
    if (runningJobs != null && runningJobs.size() >= userJobLimit) {
      String jobIds = runningJobs.stream().map(PcapStatus::getJobId).collect(Collectors.joining(", "));
      String message = String.format("Cannot submit job because a job is already running.  " +
              "Please contact the administrator to cancel job(s) with id(s) %s", jobIds);
      throw new RestException(message);
    }
    try {
      setPcapOptions(username, pcapRequest);
      pcapRequest.setFields();
      pcapJobSupplier.setPcapRequest(pcapRequest);
      JobStatus jobStatus = jobManager.submit(pcapJobSupplier, username);
      return jobStatusToPcapStatus(jobStatus);
    } catch (IOException | JobException e) {
      throw new RestException(e);
    }
  }

  @Override
  public PcapStatus getJobStatus(String username, String jobId) throws RestException {
    PcapStatus pcapStatus = null;
    try {
      Statusable<Path> statusable = jobManager.getJob(username, jobId);
      if (statusable != null) {
        pcapStatus = statusableToPcapStatus(statusable);
      }
    } catch (JobNotFoundException | InterruptedException e) {
      LOG.warn(String.format("Could not get job status.  Job not found for user %s with job id %s", username, jobId));
    } catch (JobException e) {
      throw new RestException(e);
    }
    return pcapStatus;
  }

  @Override
  public List<PcapStatus> getJobStatus(String username, JobStatus.State state) throws RestException {
    List<PcapStatus> pcapStatuses = new ArrayList<>();
    try {
      List<Statusable<Path>> statusables = jobManager.getJobs(username);
      if (statusables != null) {
        pcapStatuses = statusables.stream()
                .filter(statusable -> {
                  try {
                    return statusable.getStatus().getState() == state;
                  } catch (JobException e) {
                    return JobStatus.State.FAILED == state;
                  }
                })
                .map(statusable -> {
                  try {
                    return statusableToPcapStatus(statusable);
                  } catch (JobException | InterruptedException e) {
                    PcapStatus pcapStatus = new PcapStatus();
                    pcapStatus.setJobStatus(JobStatus.State.FAILED.toString());
                    pcapStatus.setDescription(e.getMessage());
                    return pcapStatus;
                  }
                })
                .collect(Collectors.toList());
      }
    } catch (JobNotFoundException e) {
      // do nothing and return null pcapStatus
    } catch (JobException e) {
      throw new RestException(e);
    }
    return pcapStatuses;
  }

  @Override
  public PcapStatus killJob(String username, String jobId) throws RestException {
    try {
      jobManager.killJob(username, jobId);
    } catch (JobNotFoundException e) {
      LOG.warn(String.format("Could not kill job.  Job not found for user %s with job id %s", username, jobId));
      return null;
    } catch (JobException e) {
      throw new RestException(e);
    }
    return getJobStatus(username, jobId);
  }

  @Override
  public Path getPath(String username, String jobId, Integer page) throws RestException {
    Path path = null;
    try {
      Statusable<Path> statusable = jobManager.getJob(username, jobId);
      if (statusable != null && statusable.isDone()) {
        Pageable<Path> pageable = statusable.get();
        if (pageable != null && page <= pageable.getSize() && page > 0) {
          path = pageable.getPage(page - 1);
        }
      }
    } catch (JobNotFoundException e) {
      LOG.warn(String.format("Could not get path for page %s.  Job not found for user %s with job id %s", page, username, jobId));
    } catch (JobException | InterruptedException e) {
      throw new RestException(e);
    }
    return path;
  }

  @Override
  public Pdml getPdml(String username, String jobId, Integer page) throws RestException {
    Pdml pdml = null;
    Path path = getPath(username, jobId, page);
    try {
      FileSystem fileSystem = getFileSystem();
      if (path!= null && fileSystem.exists(path)) {
        String scriptPath = environment.getProperty(MetronRestConstants.PCAP_PDML_SCRIPT_PATH_SPRING_PROPERTY);
        InputStream processInputStream = pcapToPdmlScriptWrapper.execute(scriptPath, fileSystem, path);
        pdml = new XmlMapper().readValue(processInputStream, Pdml.class);
        processInputStream.close();
      }
    } catch (IOException e) {
      throw new RestException(e);
    }
    return pdml;
  }

  @Override
  public InputStream getRawPcap(String username, String jobId, Integer page) throws RestException {
    InputStream inputStream = null;
    Path path = getPath(username, jobId, page);
    try {
      FileSystem fileSystem = getFileSystem();
      if (path!= null && fileSystem.exists(path)) {
        inputStream = fileSystem.open(path);
      }
    } catch (IOException e) {
      throw new RestException(e);
    }
    return inputStream;
  }

  @Override
  public Map<String, Object> getConfiguration(String username, String jobId) throws RestException {
    Map<String, Object> configuration = new HashMap<>();
    try {
      Statusable<Path> statusable = jobManager.getJob(username, jobId);
      if (statusable != null) {
        Map<String, Object> jobConfiguration = statusable.getConfiguration();
        configuration.put(PcapOptions.BASE_PATH.getKey(), PcapOptions.BASE_PATH.get(jobConfiguration, String.class));
        configuration.put(PcapOptions.FINAL_OUTPUT_PATH.getKey(), PcapOptions.FINAL_OUTPUT_PATH.get(jobConfiguration, String.class));
        configuration.put(PcapOptions.START_TIME_MS.getKey(), PcapOptions.START_TIME_MS.get(jobConfiguration, String.class));
        configuration.put(PcapOptions.END_TIME_MS.getKey(), PcapOptions.END_TIME_MS.get(jobConfiguration, String.class));
        configuration.put(PcapOptions.NUM_REDUCERS.getKey(), PcapOptions.NUM_REDUCERS.get(jobConfiguration, Integer.class));

        boolean isFixedFilter = PcapOptions.FILTER_IMPL.get(jobConfiguration, PcapFilterConfigurator.class) instanceof FixedPcapFilter.Configurator;
        if (isFixedFilter) {
          configuration.put(FixedPcapOptions.IP_SRC_ADDR.getKey(), FixedPcapOptions.IP_SRC_ADDR.get(jobConfiguration, String.class));
          configuration.put(FixedPcapOptions.IP_DST_ADDR.getKey(), FixedPcapOptions.IP_DST_ADDR.get(jobConfiguration, String.class));
          configuration.put(FixedPcapOptions.IP_SRC_PORT.getKey(), FixedPcapOptions.IP_SRC_PORT.get(jobConfiguration, String.class));
          configuration.put(FixedPcapOptions.IP_DST_PORT.getKey(), FixedPcapOptions.IP_DST_PORT.get(jobConfiguration, String.class));
          configuration.put(FixedPcapOptions.PROTOCOL.getKey(), FixedPcapOptions.PROTOCOL.get(jobConfiguration, String.class));
          configuration.put(FixedPcapOptions.PACKET_FILTER.getKey(), FixedPcapOptions.PACKET_FILTER.get(jobConfiguration, String.class));
          configuration.put(FixedPcapOptions.INCLUDE_REVERSE.getKey(), FixedPcapOptions.INCLUDE_REVERSE.get(jobConfiguration, String.class));
        } else {
          configuration.put(QueryPcapOptions.QUERY.getKey(), QueryPcapOptions.QUERY.get(jobConfiguration, String.class));
        }
      }
    } catch (JobNotFoundException e) {
      LOG.warn(String.format("Could not get job configuration.  Job not found for user %s with job id %s", username, jobId));
    } catch (JobException e) {
      throw new RestException(e);
    }
    return configuration;
  }

  protected void setPcapOptions(String username, PcapRequest pcapRequest) throws IOException {
    PcapOptions.JOB_NAME.put(pcapRequest, "jobName");
    PcapOptions.USERNAME.put(pcapRequest, username);
    PcapOptions.HADOOP_CONF.put(pcapRequest, configuration);
    PcapOptions.FILESYSTEM.put(pcapRequest, getFileSystem());

    if (pcapRequest.getBasePath() == null) {
      pcapRequest.setBasePath(environment.getProperty(MetronRestConstants.PCAP_BASE_PATH_SPRING_PROPERTY));
    }
    if (pcapRequest.getBaseInterimResultPath() == null) {
      pcapRequest.setBaseInterimResultPath(environment.getProperty(MetronRestConstants.PCAP_BASE_INTERIM_RESULT_PATH_SPRING_PROPERTY));
    }
    if (pcapRequest.getFinalOutputPath() == null) {
      pcapRequest.setFinalOutputPath(environment.getProperty(MetronRestConstants.PCAP_FINAL_OUTPUT_PATH_SPRING_PROPERTY));
    }

    PcapOptions.NUM_RECORDS_PER_FILE.put(pcapRequest, Integer.parseInt(environment.getProperty(MetronRestConstants.PCAP_PAGE_SIZE_SPRING_PROPERTY)));
  }

  protected FileSystem getFileSystem() throws IOException {
    return FileSystem.get(configuration);
  }

  protected PcapStatus statusableToPcapStatus(Statusable<Path> statusable) throws JobException, InterruptedException {
    PcapStatus pcapStatus = jobStatusToPcapStatus(statusable.getStatus());
    if (statusable.isDone()) {
      Pageable<Path> pageable = statusable.get();
      if (pageable != null) {
        pcapStatus.setPageTotal(pageable.getSize());
      }
    }
    return pcapStatus;
  }

  protected PcapStatus jobStatusToPcapStatus(JobStatus jobStatus) {
    PcapStatus pcapStatus = new PcapStatus();
    pcapStatus.setJobId(jobStatus.getJobId());
    pcapStatus.setJobStatus(jobStatus.getState().toString());
    pcapStatus.setDescription(jobStatus.getDescription());
    pcapStatus.setPercentComplete(jobStatus.getPercentComplete());
    return pcapStatus;
  }
}
