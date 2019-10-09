/*
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

package org.apache.metron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(Enclosed.class)
public class KafkaUtilsTest {
  @RunWith(MockitoJUnitRunner.class)
  public static class ZkMockedUtils {
    @Mock
    CuratorFramework client;
    @Mock
    GetChildrenBuilder childrenBuilder;
    @Mock
    GetDataBuilder dataBuilder;

    /**
     * {
     * "host": "192.168.1.148",
     * "port": 9092
     * }
     */
    @Multiline
    public static String brokerWithHostPort;

    @Test
    public void testGetEndpointsFromZookeeperHostPort() throws Exception {
      ArrayList<String> brokerIds = new ArrayList<>();
      brokerIds.add("1");

      when(client.getChildren()).thenReturn(childrenBuilder);
      when(childrenBuilder.forPath("/brokers/ids")).thenReturn(brokerIds);
      when(client.getData()).thenReturn(dataBuilder);
      when(dataBuilder.forPath("/brokers/ids/1")).thenReturn(brokerWithHostPort.getBytes(
          StandardCharsets.UTF_8));

      ArrayList<String> expected = new ArrayList<>();
      expected.add("192.168.1.148:9092");
      assertEquals(expected, (KafkaUtils.INSTANCE.getBrokersFromZookeeper(client)));
    }

    /**
     * {
     * "endpoints": ["PLAINTEXT://host1:9092", "SSL://host1:9093", "SASL_PLAINTEXT://host1:9094", "PLAINTEXTSASL://host1:9095"]
     * }
     */
    @Multiline
    public static String brokerWithEndpoints;

    @Test
    public void testGetEndpointsFromZookeeperEndpoints() throws Exception {
      ArrayList<String> brokerIds = new ArrayList<>();
      brokerIds.add("1");

      when(client.getChildren()).thenReturn(childrenBuilder);
      when(childrenBuilder.forPath("/brokers/ids")).thenReturn(brokerIds);
      when(client.getData()).thenReturn(dataBuilder);
      when(dataBuilder.forPath("/brokers/ids/1")).thenReturn(brokerWithEndpoints.getBytes(
          StandardCharsets.UTF_8));

      ArrayList<String> expected = new ArrayList<>();
      expected.add("host1:9092");
      expected.add("host1:9093");
      expected.add("host1:9094");
      expected.add("host1:9095");
      assertEquals(expected, (KafkaUtils.INSTANCE.getBrokersFromZookeeper(client)));
    }

    /**
     * {
     * "host": "192.168.1.148",
     * "port": 9092,
     * "endpoints": ["PLAINTEXT://host1:9092", "SSL://host1:9093"]
     * }
     */
    @Multiline
    public static String brokerWithHostPortAndEndpoints;

    @Test
    public void testGetEndpointsFromZookeeperHostPortAndEndpoints() throws Exception {
      ArrayList<String> brokerIds = new ArrayList<>();
      brokerIds.add("1");

      when(client.getChildren()).thenReturn(childrenBuilder);
      when(childrenBuilder.forPath("/brokers/ids")).thenReturn(brokerIds);
      when(client.getData()).thenReturn(dataBuilder);
      when(dataBuilder.forPath("/brokers/ids/1"))
          .thenReturn(brokerWithHostPortAndEndpoints.getBytes(StandardCharsets.UTF_8));

      ArrayList<String> expected = new ArrayList<>();
      expected.add("192.168.1.148:9092");
      assertEquals(expected, (KafkaUtils.INSTANCE.getBrokersFromZookeeper(client)));
    }
  }

  @RunWith(Parameterized.class)
  public static class ParameterizedEndPointParsing {
    static String[] hostnames = new String[]{"node1", "localhost", "192.168.0.1", "my.domain.com"};
    static String[] schemes = new String[]{"SSL", "PLAINTEXTSASL", "PLAINTEXT", "SASL_PLAINTEXT"};
    static String[] ports = new String[]{"6667", "9091", null};

    private String endpoint;
    private String expected;

    public ParameterizedEndPointParsing(String endpoint, String expected) {
      this.endpoint = endpoint;
      this.expected = expected;
    }

    @Parameters(name = "{index}:endpoint({0}={1})")
    public static Collection<Object[]> data() {
      List<Object[]> ret = new ArrayList<>();
      for (String scheme : schemes) {
        for (String hostname : hostnames) {
          for (String port : ports) {
            port = port != null ? (":" + port) : "";
            String expected = hostname + port;
            ret.add(new Object[]{scheme + "://" + expected, expected});
          }
        }
      }
      return ret;
    }

    @Test
    public void testEndpointParsing() throws URISyntaxException {
      assertEquals(expected, KafkaUtils.INSTANCE.fromEndpoint(endpoint).get(0));
    }
  }
}
