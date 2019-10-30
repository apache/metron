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
package org.apache.metron.maas.discovery;

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
import org.apache.metron.maas.config.Model;
import org.apache.metron.maas.config.ModelEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceDiscoveryIntegrationTest {
  private TestingServer testZkServer;
  private String zookeeperUrl;
  private CuratorFramework client;
  private ServiceDiscoverer discoverer;

  @BeforeEach
  public void setup() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
    client.start();
    discoverer = new ServiceDiscoverer(client, "/maas/discover");
    discoverer.start();
  }

  private ServiceInstance<ModelEndpoint> createInstance(ModelEndpoint ep) throws Exception {
    URL url = new URL(ep.getEndpoint().getUrl());
    ServiceInstanceBuilder<ModelEndpoint> builder = ServiceInstance.<ModelEndpoint> builder()
            .address(url.getHost())
            .id(ep.getContainerId())
            .name(ep.getName())
            .port(url.getPort())
            .registrationTimeUTC(System.currentTimeMillis())
            .serviceType(ServiceType.STATIC)
            .payload(ep)
            ;
    return builder.build();
  }

  private void registerService(ModelEndpoint ep) throws Exception {
    discoverer.getServiceDiscovery().registerService(createInstance(ep));
  }
  private void registerService(String name, String version, AtomicInteger containerId) throws Exception {
    ModelEndpoint ep = new ModelEndpoint();
    ep.setName(name);
    ep.setVersion(version);
    ep.setContainerId(containerId.incrementAndGet() + "");
    ep.setEndpoint(new Endpoint() {{
              setUrl("http://localhost:9080/ep1");
                   }}
    );
    registerService(ep);
  }
  @Test
  public void testDiscovery() throws Exception {
    //register some models
    AtomicInteger containerId = new AtomicInteger(0);
    registerService("casey", "3.14159", containerId);
    registerService("casey", "3.14159", containerId);
    registerService("casey", "3.14159", containerId);
    registerService("casey", "3.1416", containerId);
    //wait for zk to percolate the changes.
    Thread.sleep(2000);

    assertEquals(3, discoverer.getEndpoints(new Model("casey", "3.14159")).size());
    assertEquals(1, discoverer.getEndpoints(new Model("casey", "3.1416")).size());
    assertEquals(0, discoverer.getEndpoints(new Model("casey", "3.17")).size());

    discoverer.unregisterByContainer("1");
    Thread.sleep(2000);
    assertEquals(2, discoverer.getEndpoints(new Model("casey", "3.14159")).size());
    assertEquals(1, discoverer.getEndpoints(new Model("casey", "3.1416")).size());
    assertEquals(0, discoverer.getEndpoints(new Model("casey", "3.17")).size());

    assertEquals(2, discoverer.listEndpoints(new Model("casey", null)).keySet().size());
    assertEquals(1, discoverer.listEndpoints(new Model("casey", "3.1416")).keySet().size());
    assertEquals(1, discoverer.listEndpoints(new Model("casey", "3.1416"))
                                     .get(new Model("casey", "3.1416")).size()
                       );
    assertEquals("4", discoverer.listEndpoints(new Model("casey", "3.1416"))
                                       .get(new Model("casey", "3.1416"))
                                       .get(0)
                                       .getContainerId()
                       );
    assertEquals(0, discoverer.listEndpoints(new Model("casey", "3.17")).keySet().size());
    assertEquals(0, discoverer.listEndpoints(new Model("dummy", null)).keySet().size());

  }
  @AfterEach
  public void teardown() throws Exception {

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
