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

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.PcapMerger;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.rest.model.PcapsResponse;
import org.apache.metron.rest.service.PcapQueryService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PcapQueryServiceImpl implements PcapQueryService {

  @Override
  public PcapsResponse query() {
    PcapsResponse response = new PcapsResponse();
    PcapJob pcapJob = new PcapJob();
    Configuration configuration = new Configuration();
    try {
      SequenceFileIterable results = pcapJob.query(
              new Path("/apps/metron/pcap"),
              new Path("/tmp/pcap"),
              TimestampConverters.MILLISECONDS.toNanoseconds(0L),
              TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis()),
              1,
              "query",
              configuration,
              FileSystem.get(configuration),
              new QueryPcapFilter.Configurator()
      );
      response.setPcaps(results != null ? Lists.newArrayList(results) : null);
    } catch (IOException | ClassNotFoundException | InterruptedException e) {
      e.printStackTrace();
    }
    return response;
  }

  protected byte[] mergePcaps(List<byte[]> pcaps) throws IOException {
    if (pcaps.size() == 1) {
      return pcaps.get(0);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PcapMerger.merge(baos, pcaps);
    return baos.toByteArray();

  }
}
