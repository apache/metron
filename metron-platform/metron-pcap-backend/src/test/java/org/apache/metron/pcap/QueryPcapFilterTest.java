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

package org.apache.metron.pcap;

import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.Constants;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.EnumMap;
import java.util.HashMap;

public class QueryPcapFilterTest {

  @Test
  public void testEmptyQueryFilter() throws Exception {
    Configuration config = new Configuration();
    String query = "";
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      PcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
  }

  @Test
  public void testTrivialEquality() throws Exception {
    Configuration config = new Configuration();
    String query = "ip_src_addr == 'src_ip' and ip_src_port == 0 and ip_dst_addr == 'dst_ip' and ip_dst_port == 1";
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      PcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
  }

  @Test
  public void testMissingDstAddr() throws Exception {
    Configuration config = new Configuration();
    String query = "ip_src_addr == 'src_ip' and ip_src_port == 0 and ip_dst_port == 1";
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip_no_match");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertFalse(filter.test(null));
    }
  }

  @Test
  public void testMissingDstPort() throws Exception {
    Configuration config = new Configuration();
    String query = "ip_src_addr == 'src_ip' and ip_src_port == 0 and ip_dst_addr == 'dst_ip'";
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 100);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 100);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 100);
          }};
        }
      };
      filter.configure(config);
      Assert.assertFalse(filter.test(null));
    }
  }

  @Test
  public void testMissingSrcAddr() throws Exception {
    Configuration config = new Configuration();
    String query = "ip_src_port == 0 and ip_dst_addr == 'dst_ip' and ip_dst_port == 1";
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
  }

  @Test
  public void testMissingSrcPort() throws Exception {
    Configuration config = new Configuration();
    String query = "ip_src_addr == 'src_ip' and ip_dst_addr == 'dst_ip' and ip_dst_port == 1";
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
    new QueryPcapFilter.Configurator().addToConfig(query, config);
    {
      QueryPcapFilter filter = new QueryPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 100);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      Assert.assertTrue(filter.test(null));
    }
  }
}
