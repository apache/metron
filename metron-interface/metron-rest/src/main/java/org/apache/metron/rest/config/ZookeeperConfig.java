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
package org.apache.metron.rest.config;

import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("!test")
public class ZookeeperConfig {

  public static final String ZK_URL_SPRING_PROPERTY = "zookeeper.url";
  public static final String ZK_CLIENT_SESSION_TIMEOUT = "zookeeper.client.timeout.session";
  public static final String ZK_CLIENT_CONNECTION_TIMEOUT = "zookeeper.client.timeout.connection";
  public static final String CURATOR_SLEEP_TIME = "curator.sleep.time";
  public static final String CURATOR_MAX_RETRIES = "curator.max.retries";

  @Bean(initMethod = "start", destroyMethod="close")
  public CuratorFramework client(Environment environment) {
    int sleepTime = Integer.parseInt(environment.getProperty(CURATOR_SLEEP_TIME));
    int maxRetries = Integer.parseInt(environment.getProperty(CURATOR_MAX_RETRIES));
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(sleepTime, maxRetries);
    return CuratorFrameworkFactory.newClient(environment.getProperty(ZK_URL_SPRING_PROPERTY), retryPolicy);
  }

  @Bean(destroyMethod="close")
  public ZkClient zkClient(Environment environment) {
    int sessionTimeout = Integer.parseInt(environment.getProperty(ZK_CLIENT_SESSION_TIMEOUT));
    int connectionTimeout = Integer.parseInt(environment.getProperty(ZK_CLIENT_CONNECTION_TIMEOUT));
    return new ZkClient(environment.getProperty(ZK_URL_SPRING_PROPERTY), sessionTimeout, connectionTimeout, ZKStringSerializer$.MODULE$);
  }
}
