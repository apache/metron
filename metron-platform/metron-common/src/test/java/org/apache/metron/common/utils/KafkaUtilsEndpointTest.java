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

package org.apache.metron.common.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class KafkaUtilsEndpointTest {
  static String[] hostnames = new String[] { "node1", "localhost", "192.168.0.1", "my.domain.com" };
  static String[] schemes = new String[] { "SSL", "PLAINTEXTSASL", "PLAINTEXT"};
  static String[] ports = new String[] { "6667", "9091", null};
  private String endpoint;
  private String expected;

  public KafkaUtilsEndpointTest(String endpoint, String expected) {
    this.endpoint = endpoint;
    this.expected = expected;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    List<Object[]> ret = new ArrayList<>();
    for(String scheme : schemes) {
      for(String hostname : hostnames) {
        for(String port : ports) {
          port = port != null?(":" + port):"";
          String expected = hostname + port;
          ret.add(new Object[]{scheme + "://" + expected, expected });
        }
      }
    }
    return ret;
  }

  @Test
  public void testEndpointParsing() throws URISyntaxException {
    Assert.assertEquals(expected, KafkaUtils.INSTANCE.fromEndpoint(endpoint).get(0));
  }

}
