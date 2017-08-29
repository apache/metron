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
package org.apache.metron.parsers.bolt;

import java.net.URISyntaxException;
import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.bundles.*;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads a Parser from the Metron Bundle Extension System.
 * This Bundle by be resident on the local filesystem or it may located in HDFS.
 *
 */
public class ParserLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ParserBolt.class);

  /**
   * Loads a parser from a configuration
   * @param stormConfig the storm config
   * @param client the CuratorFramework
   * @param parserConfig the configuration
   * @return Optional of MessageParser<JSONObject>
   */
  @SuppressWarnings("unchecked")
  public static Optional<MessageParser<JSONObject>> loadParser(Map stormConfig,
      CuratorFramework client, SensorParserConfig parserConfig) {
    MessageParser<JSONObject> parser = null;
    try {
      // fetch the BundleProperties from zookeeper
      Optional<BundleProperties> bundleProperties = getBundleProperties(client);
      if (bundleProperties.isPresent()) {
        BundleProperties props = bundleProperties.get();
        BundleSystem bundleSystem = new BundleSystem.Builder().withBundleProperties(props).build();
        parser = bundleSystem
            .createInstance(parserConfig.getParserClassName(), MessageParser.class);
      } else {
        LOG.error("BundleProperties are missing!");
      }
    } catch (Exception e) {
      LOG.error("Failed to load parser " + parserConfig.getParserClassName(), e);
      return Optional.empty();
    }
    return Optional.of(parser);
  }

  private static Optional<BundleProperties> getBundleProperties(CuratorFramework client)
      throws Exception {
    BundleProperties properties = null;
    byte[] propBytes = ConfigurationsUtils
        .readFromZookeeper(Constants.ZOOKEEPER_ROOT + "/bundle.properties", client);
    if (propBytes.length > 0) {
      // read in the properties
      properties = BundleProperties
          .createBasicBundleProperties(new ByteArrayInputStream(propBytes), new HashMap<>());
    }
    return Optional.of(properties);
  }
}
