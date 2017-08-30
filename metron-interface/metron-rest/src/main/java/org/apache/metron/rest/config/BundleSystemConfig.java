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
import java.net.URI;
import java.util.HashMap;
import java.util.Optional;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.bundles.BundleSystem;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.rest.MetronRestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

public class BundleSystemConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  public BundleSystemConfig() {}

  public static BundleSystem bundleSystem(CuratorFramework client) throws Exception {
    Optional<BundleProperties> properties = getBundleProperties(client);
    if (!properties.isPresent()) {
      throw new IllegalStateException("BundleProperties are not available");
    }
    return new BundleSystem.Builder().withBundleProperties(properties.get()).build();
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
