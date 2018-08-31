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

package org.apache.metron.pcap.mr;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.junit.Test;

public class OutputDirFormatterTest {

  @Test
  public void formats_directory_name_for_query_filter_types() throws Exception {
    long beginNS = TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis());
    long endNS = TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis());
    String query = "ip_dst_addr == '207.28.210.1' and protocol == 'PROTOCOL: ICMP(1)";
    String queryFilterString = new QueryPcapFilter.Configurator().queryToString(query);
    OutputDirFormatter formatter = new OutputDirFormatter();
    String actual = formatter.format(beginNS, endNS, queryFilterString);
    assertThat("Formatted directory names did not match.", actual, containsString("_ip_dst_addr_==_207-28-210-1_and_protocol_==_PROTOCOL_ICMP(1)_"));
    // no URI exception should be thrown with dir name
    new Path(actual);
  }

  @Test
  public void formats_directory_name_for_fixed_filter_types() throws Exception {
    long beginNS = TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis());
    long endNS = TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis());
    Map<String, String> fields = new HashMap<>();
    fields.put("ip_src_address", "207.28.210.1");
    fields.put("protocol", "PROTOCOL: ICMP(1)");
    String fixedFilterString = new FixedPcapFilter.Configurator().queryToString(fields);
    OutputDirFormatter formatter = new OutputDirFormatter();
    String actual = formatter.format(beginNS, endNS, fixedFilterString);
    assertThat("Formatted directory names did not match.", actual, containsString("PROTOCOL_ICMP(1)_207-28-210-1"));
    // no URI exception should be thrown with dir name
    new Path(actual);
  }

}
