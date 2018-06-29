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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.job.Pageable;
import org.apache.metron.pcap.PcapFiles;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.pcap.writer.ResultsWriter;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.apache.metron.rest.model.pcap.Pdml;
import org.apache.metron.rest.service.PcapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PcapServiceImpl implements PcapService {

  private Environment environment;
  private Configuration configuration;
  private PcapJob pcapJob;

  public PcapServiceImpl() {

  }

  @Autowired
  public PcapServiceImpl(Environment environment, Configuration configuration, PcapJob pcapJob) {
    this.environment = environment;
    this.configuration = configuration;
    this.pcapJob = pcapJob;
  }

  @Override
  public List<String> fixed(FixedPcapRequest fixedPcapRequest) throws RestException {
    if (fixedPcapRequest.getBasePath() == null) {
      fixedPcapRequest.setBasePath(environment.getProperty(MetronRestConstants.PCAP_INPUT_PATH_SPRING_PROPERTY));
    }
    if (fixedPcapRequest.getBaseOutputPath() == null) {
      fixedPcapRequest.setBaseOutputPath(environment.getProperty(MetronRestConstants.PCAP_OUTPUT_PATH_SPRING_PROPERTY));
    }
    List<String> paths = new ArrayList<>();
    SequenceFileIterable results;
    try {
      results = pcapJob.query(
              new Path(fixedPcapRequest.getBasePath()),
              new Path(fixedPcapRequest.getBaseOutputPath()),
              TimestampConverters.MILLISECONDS.toNanoseconds(fixedPcapRequest.getStartTime()),
              TimestampConverters.MILLISECONDS.toNanoseconds(fixedPcapRequest.getEndTime()),
              fixedPcapRequest.getNumReducers(),
              getFixedFields(fixedPcapRequest),
              configuration,
              getFileSystem(),
              new FixedPcapFilter.Configurator()
      );
      if (results != null) {
        Path outputPath = new Path("file:///tmp");
        int recPerFile = Integer.parseInt(environment.getProperty(MetronRestConstants.PCAP_PAGE_SIZE_SPRING_PROPERTY, "100"));
        String prefix = new SimpleDateFormat("yyyyMMddHHmm").format(Calendar.getInstance().getTime());
        Pageable<Path> pages = pcapJob.writeResults(results, new ResultsWriter(), outputPath, recPerFile, prefix);
        pages.asIterable().iterator().forEachRemaining(path -> paths.add(path.toUri().getPath()));
      }
    } catch (IOException | ClassNotFoundException | InterruptedException e) {
      throw new RestException(e);
    }
    return paths;
  }

  @Override
  public Pdml getPdml(Path path) throws RestException {
    Pdml pdml = null;
    if (new File(path.toString()).exists()) {
      ProcessBuilder processBuilder = getProcessBuilder(environment.getProperty(MetronRestConstants.PCAP_PDML_SCRIPT_PATH_SPRING_PROPERTY), path.toUri().getPath());
      Process process;
      try {
        process = processBuilder.start();
        InputStream inputStream = process.getInputStream();
        byte[] processOutput = IOUtils.toByteArray(inputStream);
        inputStream.close();
        process.waitFor(30, TimeUnit.SECONDS);
        if (process.exitValue() == 0) {
          pdml = pdmlXmlToJson(processOutput);
        } else {
          String errorMessage = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
          throw new RestException(errorMessage);
        }
      } catch (IOException | InterruptedException e) {
        throw new RestException(e);
      }
    }
    return pdml;
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

  protected Pdml pdmlXmlToJson(byte[] xml) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    Pdml node = xmlMapper.readValue(xml, Pdml.class);
    return node;
//    ObjectMapper jsonMapper = new ObjectMapper();
//    return JSONUtils.INSTANCE.load(jsonMapper.writeValueAsString(node), Pdml.class);
  }

  protected FileSystem getFileSystem() throws IOException {
    return FileSystem.get(configuration);
  }

  protected ProcessBuilder getProcessBuilder(String... command) {
    return new ProcessBuilder(command);
  }

  public static void main(String[] args) throws RestException {
    PcapService pcapService = new PcapServiceImpl();
    Pdml pdml = pcapService.getPdml(new Path("/Users/rmerriman/Projects/Metron/code/forks/merrimanr/samplePcapData/pcap-data-201806272004-289365c53112438ca55ea047e13a12a5+0001.pcap"));
    System.out.println(pdml);
  }
}
