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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.PcapsResponse;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.apache.metron.rest.service.PcapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PcapServiceImpl implements PcapService {

  private Configuration configuration;
  private PcapJob pcapJob;

  @Autowired
  public PcapServiceImpl(Configuration configuration, PcapJob pcapJob) {
    this.configuration = configuration;
    this.pcapJob = pcapJob;
  }

  @Override
  public PcapsResponse fixed(FixedPcapRequest fixedPcapRequest) throws RestException, IOException {
    PcapsResponse response = new PcapsResponse();
    SequenceFileIterable results = null;
    try {
      results = pcapJob.query(
              new Path(fixedPcapRequest.getBasePath()),
              new Path(fixedPcapRequest.getBaseOutputPath()),
              TimestampConverters.MILLISECONDS.toNanoseconds(fixedPcapRequest.getStartTime()),
              TimestampConverters.MILLISECONDS.toNanoseconds(fixedPcapRequest.getEndTime()),
              fixedPcapRequest.getNumReducers(),
              getFixedFields(fixedPcapRequest),
              configuration,
              FileSystem.get(configuration),
              new FixedPcapFilter.Configurator()
      );
      if (results != null) {
        List<byte[]> pcaps = new ArrayList<>();
        results.iterator().forEachRemaining(pcaps::add);
        response.setPcaps(pcaps);
      }
    } catch (IOException | ClassNotFoundException | InterruptedException e) {
      throw new RestException(e);
    } finally {
      if (null != results) {
        results.cleanup();
      }
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
      fixedFields.put(Constants.Fields.SRC_PORT.getName(), fixedPcapRequest.getIpSrcPort());
    }
    if (fixedPcapRequest.getIpDstPort() != null) {
      fixedFields.put(Constants.Fields.DST_PORT.getName(), fixedPcapRequest.getIpDstPort());
    }
    if (fixedPcapRequest.getProtocol() != null) {
      fixedFields.put(Constants.Fields.PROTOCOL.getName(), fixedPcapRequest.getProtocol());
    }
    if (fixedPcapRequest.getIncludeReverse() != null) {
      fixedFields.put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), fixedPcapRequest.getIncludeReverse());
    }
    if (fixedPcapRequest.getPacketFilter() != null) {
      fixedFields.put(PcapHelper.PacketFields.PACKET_FILTER.getName(), fixedPcapRequest.getPacketFilter());
    }
    return fixedFields;
  }
}
