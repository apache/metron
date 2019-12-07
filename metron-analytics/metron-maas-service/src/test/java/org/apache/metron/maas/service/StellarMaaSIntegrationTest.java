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
package org.apache.metron.maas.service;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.server.impl.application.WebApplicationImpl;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.metron.maas.config.Endpoint;
import org.apache.metron.maas.config.MaaSConfig;
import org.apache.metron.maas.config.ModelEndpoint;
import org.apache.metron.maas.discovery.ServiceDiscoverer;
import org.apache.metron.maas.util.ConfigUtil;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.junit.jupiter.api.Assertions.*;

public class StellarMaaSIntegrationTest {
  private static Context context;
  private static TestingServer testZkServer;
  private static String zookeeperUrl;
  private static CuratorFramework client;
  private static ServiceDiscoverer discoverer;
  private static URL endpointUrl;

  @BeforeAll
  public static void setup() throws Exception {
    UnitTestHelper.setJavaLoggingLevel(WebApplicationImpl.class, Level.WARNING);
    MockDGAModel.start(8282);
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
    client.start();
    context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .build();
    MaaSConfig config = ConfigUtil.INSTANCE.read(client, "/metron/maas/config", new MaaSConfig(), MaaSConfig.class);
    discoverer = new ServiceDiscoverer(client, config.getServiceRoot());
    discoverer.start();
    endpointUrl = new URL("http://localhost:8282");
    ModelEndpoint endpoint = new ModelEndpoint();
    {
      endpoint.setName("dga");
      endpoint.setContainerId("0");
      Endpoint ep = new Endpoint();
      ep.setUrl(endpointUrl.toString());
      endpoint.setEndpoint(ep);
      endpoint.setVersion("1.0");
    }
    ;

    ServiceInstanceBuilder<ModelEndpoint> builder = ServiceInstance.<ModelEndpoint>builder()
            .address(endpointUrl.getHost())
            .id("0")
            .name("dga")
            .port(endpointUrl.getPort())
            .registrationTimeUTC(System.currentTimeMillis())
            .serviceType(ServiceType.STATIC)
            .payload(endpoint);
    final ServiceInstance<ModelEndpoint> instance = builder.build();
    discoverer.getServiceDiscovery().registerService(instance);
    //wait til the endpoint is installed...
    for(int i = 0;i < 10;++i) {
      try {
        Object o = discoverer.getEndpoint("dga");
        if(o != null) {
          break;
        }
      }
      catch(Exception e) {

      }
      Thread.sleep(1000);
    }
  }

  @Test
  public void testGetEndpointWithoutVersion() {
    String stellar = "MAAS_GET_ENDPOINT('dga')";
    Object result = run(stellar, new HashMap<>(), context);
    assertTrue(result instanceof Map);
    Map<String, String> resMap = (Map<String, String>)result;
    assertEquals(resMap.get("url"), "http://localhost:8282");
    assertEquals(resMap.get("name"), "dga");
    assertEquals(resMap.get("version"), "1.0");
    assertEquals(resMap.get("endpoint:apply"), "apply");

  }

  @Test
  public void testGetEndpointWithVersion() {
    String stellar = "MAAS_GET_ENDPOINT('dga', '1.0')";
    Object result = run(stellar, new HashMap<>(), context);
    assertTrue(result instanceof Map);
    Map<String, String> resMap = (Map<String, String>)result;
    assertEquals(resMap.get("url"), "http://localhost:8282");
    assertEquals(resMap.get("name"), "dga");
    assertEquals(resMap.get("version"), "1.0");
    assertEquals(resMap.get("endpoint:apply"), "apply");
  }

  @Test
  public void testGetEndpointWithWrongVersion() {
    String stellar = "MAAS_GET_ENDPOINT('dga', '2.0')";
    Object result = run(stellar, new HashMap<>(), context);
    assertNull(result);
  }

  @Test
  public void testModelApply() {
    {
      String stellar = "MAP_GET('is_malicious', MAAS_MODEL_APPLY(MAAS_GET_ENDPOINT('dga'), {'host': host}))";
      Object result = run(stellar, ImmutableMap.of("host", "badguy.com"), context);
      assertTrue((Boolean) result);
    }
    {
      String stellar = "MAP_GET('is_malicious', MAAS_MODEL_APPLY(MAAS_GET_ENDPOINT('dga'), {'host': host}))";
      Object result = run(stellar, ImmutableMap.of("host", "youtube.com"), context);
      assertFalse((Boolean) result);
    }
    {
      String stellar = "MAP_GET('is_malicious', MAAS_MODEL_APPLY(MAAS_GET_ENDPOINT('dga'), 'apply', {'host': host}))";
      Object result = run(stellar, ImmutableMap.of("host", "youtube.com"), context);
      assertFalse((Boolean) result);
    }

  }

  @Test
  public void testModelApplyNegative() {
    {
      String stellar = "MAP_GET('is_malicious', MAAS_MODEL_APPLY(MAAS_GET_ENDPOINT('dga', '2.0'), {'host': host}))";
      Object result = run(stellar, ImmutableMap.of("host", "youtube.com"), context);
      assertNull( result);
    }
  }

  @AfterAll
  public static void teardown() {
    MockDGAModel.shutdown();
    if(discoverer != null) {
      CloseableUtils.closeQuietly(discoverer);
    }
    if(client != null) {
      CloseableUtils.closeQuietly(client);
    }
    if(testZkServer != null) {
      CloseableUtils.closeQuietly(testZkServer);
    }
  }
}
