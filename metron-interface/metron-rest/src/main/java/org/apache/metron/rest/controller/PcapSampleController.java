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
package org.apache.metron.rest.controller;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.rest.RestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// NOTE:  This is meant only as an example and should be replaced as part of https://issues.apache.org/jira/browse/METRON-1559
@RestController
@RequestMapping("/api/v1/pcap/sample")
public class PcapSampleController {

  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<PcapsResponse> query() throws RestException {
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
              "",
              configuration,
              FileSystem.get(configuration),
              new QueryPcapFilter.Configurator()
      );
      response.setPcaps(results != null ? Lists.newArrayList(results) : null);
    } catch (IOException | ClassNotFoundException | InterruptedException e) {
      e.printStackTrace();
    }
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  /**
   * Holds pcaps data, status and the partial response key.
   *
   * @author Sayi
   */
  class PcapsResponse {

    /** response of the processed keys. */
    private List<byte[]> pcaps = new ArrayList<byte[]>();;

    /**
     * Sets the pcaps.
     *
     * @param pcaps
     *          the new pcaps
     */
    public void setPcaps(List<byte[]> pcaps) {
      this.pcaps = pcaps;
    }

    /**
     * Adds the pcaps.
     *
     * @param pcaps
     *          the pcaps
     */
    public void addPcaps(byte[] pcaps) {
      this.pcaps.add(pcaps);
    }


    /**
     * Gets the response size.
     *
     * @return the response size
     */
    public long getResponseSize() {
      long responseSize = 0;
      for (byte[] pcap : this.pcaps) {
        responseSize = responseSize + pcap.length;
      }
      return responseSize;
    }

    /**
     * Gets the pcaps.
     *
     * @return the pcaps
     * @throws IOException
     *           Signals that an I/O exception has occurred.
     */
    public List<byte[]> getPcaps() throws IOException {
      if(pcaps == null) {
        return new ArrayList<>();
      } else {
        return pcaps;
      }
    }
  }
}
