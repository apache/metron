/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.rest.config;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.bundles.BundleSystem;
import org.apache.metron.bundles.BundleSystemBuilder;
import org.apache.metron.bundles.BundleSystemType;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleSystemConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  public BundleSystemConfig() {}

  public static BundleSystem bundleSystem(CuratorFramework client) throws Exception {
    Optional<BundleProperties> properties = getBundleProperties(client);
    if (!properties.isPresent()) {
      throw new IllegalStateException("BundleProperties are not available");
    }
    // we require an ON_DEMAND BundleSystem that will not be created until needed
    // The reason is, when we create the BundleSystem here in the bean at initialization time,
    // the VFS System seems to have trouble reading from hdfs
    return new BundleSystemBuilder().withBundleProperties(properties.get()).withBundleSystemType(
        BundleSystemType.ON_DEMAND).build();
  }

  private static Optional<BundleProperties> getBundleProperties(CuratorFramework client)
      throws Exception {
    BundleProperties properties = null;
    byte[] propBytes = ConfigurationsUtils
        .readFromZookeeper(Constants.ZOOKEEPER_ROOT + "/bundle.properties", client);
    if (propBytes.length > 0) {
      properties = BundleProperties
          .createBasicBundleProperties(new ByteArrayInputStream(propBytes), new HashMap<>());
    }
    return Optional.of(properties);
  }
}
