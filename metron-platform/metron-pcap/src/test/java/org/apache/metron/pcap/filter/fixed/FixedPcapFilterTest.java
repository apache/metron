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

package org.apache.metron.pcap.filter.fixed;

import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.Constants;
import org.apache.metron.pcap.PacketInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FixedPcapFilterTest {

  @Test
  public void string_representation_of_query_gets_formatted() {
    final LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
      put(Constants.Fields.SRC_PORT.getName(), "0");
      put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
      put(Constants.Fields.DST_PORT.getName(), "1");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
    }};
    String actual = new FixedPcapFilter.Configurator().queryToString(fields);
    String expected = "src_ip_0_dst_ip_1_false";
    assertThat("string representation did not match", actual, equalTo(expected));
  }

  @Test
  public void string_representation_of_empty_fields_empty() {
    {
      final LinkedHashMap<String, String> fields = new LinkedHashMap<>();
      String actual = new FixedPcapFilter.Configurator().queryToString(fields);
      String expected = "";
      assertThat("string representation did not match", actual, equalTo(expected));
    }
    {
      String actual = new FixedPcapFilter.Configurator().queryToString(null);
      String expected = "";
      assertThat("string representation did not match", actual, equalTo(expected));
    }
    {
      final LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>() {{
        put(Constants.Fields.SRC_ADDR.getName(), "");
        put(Constants.Fields.SRC_PORT.getName(), "");
      }};
      String actual = new FixedPcapFilter.Configurator().queryToString(fields);
      String expected = "_";
      assertThat("string representation did not match", actual, equalTo(expected));
    }
  }

  @Test
  public void testTrivialEquality() {
    Configuration config = new Configuration();
    final Map<String, String> fields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
      put(Constants.Fields.SRC_PORT.getName(), "0");
      put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
      put(Constants.Fields.DST_PORT.getName(), "1");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
    }};
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
        @Override
        protected Map<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      assertTrue(filter.test(null));
    }
  }

  @Test
  public void testReverseTraffic() {
    Configuration config = new Configuration();
    final Map<String, String> fields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
      put(Constants.Fields.SRC_PORT.getName(), "0");
      put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
      put(Constants.Fields.DST_PORT.getName(), "1");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "true");
    }};
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
        @Override
        protected Map<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      assertTrue(filter.test(null));
    }
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
        @Override
        protected Map<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "dst_ip");
            put(Constants.Fields.SRC_PORT.getName(), 1);
            put(Constants.Fields.DST_ADDR.getName(), "src_ip");
            put(Constants.Fields.DST_PORT.getName(), 0);
          }};
        }
      };
      filter.configure(config);
      assertTrue(filter.test(null));
    }
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
        @Override
        protected Map<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "dst_ip");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "src_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      assertFalse(filter.test(null));
    }
  }

  @Test
  public void testMissingDstAddr() {
    Configuration config = new Configuration();
    final HashMap<String, String> fields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
      put(Constants.Fields.SRC_PORT.getName(), "0");
      put(Constants.Fields.DST_PORT.getName(), "1");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
    }};
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
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
      assertTrue(filter.test(null));
    }
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
        @Override
        protected HashMap<String, Object> packetToFields(PacketInfo pi) {
          return new HashMap<String, Object>() {{
            put(Constants.Fields.SRC_ADDR.getName(), "src_ip1");
            put(Constants.Fields.SRC_PORT.getName(), 0);
            put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
            put(Constants.Fields.DST_PORT.getName(), 1);
          }};
        }
      };
      filter.configure(config);
      assertFalse(filter.test(null));
    }
  }

  @Test
  public void testMissingDstPort() {
    Configuration config = new Configuration();
    final HashMap<String, String> fields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
      put(Constants.Fields.SRC_PORT.getName(), "0");
      put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
    }};
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
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
      assertTrue(filter.test(null));
    }
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
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
      assertTrue(filter.test(null));
    }
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
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
      assertFalse(filter.test(null));
    }
  }

  @Test
  public void testMissingSrcAddr() {
    Configuration config = new Configuration();
    final HashMap<String, String> fields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_PORT.getName(), "0");
      put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
      put(Constants.Fields.DST_PORT.getName(), "1");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
    }};
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
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
      assertTrue(filter.test(null));
    }
  }

  @Test
  public void testMissingSrcPort() {
    Configuration config = new Configuration();
    final HashMap<String, String> fields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "src_ip");
      put(Constants.Fields.DST_ADDR.getName(), "dst_ip");
      put(Constants.Fields.DST_PORT.getName(), "1");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
    }};
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
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
      assertTrue(filter.test(null));
    }
    new FixedPcapFilter.Configurator().addToConfig(fields, config);
    {
      FixedPcapFilter filter = new FixedPcapFilter() {
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
      assertTrue(filter.test(null));
    }
  }

}
