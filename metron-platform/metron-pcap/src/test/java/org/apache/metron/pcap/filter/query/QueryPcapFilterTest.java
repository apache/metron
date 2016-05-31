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

package org.apache.metron.pcap.filter.query;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class QueryPcapFilterTest {

  @Test
  public void string_representation_of_query_gets_formatted() throws Exception {
    String query = "ip_src_addr == 'srcIp' and ip_src_port == '80' and ip_dst_addr == 'dstIp' and ip_dst_port == '100' and protocol == 'protocol'";
    String actual = new QueryPcapFilter.Configurator().queryToString(query);
    String expected = "ip_src_addr_==_srcIp_and_ip_src_port_==_80_and_ip_dst_addr_==_dstIp_and_ip_dst_port_==_100_and_protocol_==_protocol";
    Assert.assertThat("string representation did not match", actual, equalTo(expected));
  }

  @Test
  public void string_representation_of_empty_query_empty() throws Exception {
    {
      String query = "";
      String actual = new QueryPcapFilter.Configurator().queryToString(query);
      String expected = "";
      Assert.assertThat("string representation did not match", actual, equalTo(expected));
    }
    {
      String query = " ";
      String actual = new QueryPcapFilter.Configurator().queryToString(query);
      String expected = "";
      Assert.assertThat("string representation did not match", actual, equalTo(expected));
    }
    {
      String query = null;
      String actual = new QueryPcapFilter.Configurator().queryToString(query);
      String expected = "";
      Assert.assertThat("string representation did not match", actual, equalTo(expected));
    }
  }

}
