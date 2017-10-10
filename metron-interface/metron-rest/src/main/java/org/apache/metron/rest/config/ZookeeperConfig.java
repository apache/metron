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
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.common.zookeeper.ZKConfigurationsCache;
import org.apache.metron.rest.MetronRestConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

@Configuration
@Profile("!" + TEST_PROFILE)
public class ZookeeperConfig {

  @Bean(initMethod = "start", destroyMethod="close")
  public ConfigurationsCache cache(CuratorFramework client) {
    return new ZKConfigurationsCache( client
                                    , ZKConfigurationsCache.ConfiguredTypes.ENRICHMENT
                                    , ZKConfigurationsCache.ConfiguredTypes.PARSER
                                    , ZKConfigurationsCache.ConfiguredTypes.INDEXING
                                    );
  }

  @Bean(initMethod = "start", destroyMethod="close")
  public CuratorFramework client(Environment environment) {
    int sleepTime = Integer.parseInt(environment.getProperty(MetronRestConstants.CURATOR_SLEEP_TIME));
    int maxRetries = Integer.parseInt(environment.getProperty(MetronRestConstants.CURATOR_MAX_RETRIES));
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(sleepTime, maxRetries);
    CuratorFramework ret = CuratorFrameworkFactory.newClient(environment.getProperty(MetronRestConstants.ZK_URL_SPRING_PROPERTY), retryPolicy);
    return ret;
  }

  @Bean(destroyMethod="close")
  public ZkClient zkClient(Environment environment) {
    int sessionTimeout = Integer.parseInt(environment.getProperty(MetronRestConstants.ZK_CLIENT_SESSION_TIMEOUT));
    int connectionTimeout = Integer.parseInt(environment.getProperty(MetronRestConstants.ZK_CLIENT_CONNECTION_TIMEOUT));
    return new ZkClient(environment.getProperty(MetronRestConstants.ZK_URL_SPRING_PROPERTY), sessionTimeout, connectionTimeout, ZKStringSerializer$.MODULE$);
  }
}
