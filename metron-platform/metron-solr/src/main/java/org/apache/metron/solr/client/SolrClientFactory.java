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

package org.apache.metron.solr.client;

import com.google.common.base.Splitter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;

/**
 * Factory for creating a SolrClient.  The default implementation of SolrClient is CloudSolrClient.
 */
public class SolrClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static SolrClient solrClient;

  /**
   * Creates a SolrClient.
   * @param globalConfig Global config
   * @return SolrClient
   */
  public static SolrClient create(Map<String, Object> globalConfig) {
    if (solrClient == null) {
      synchronized (SolrClientFactory.class) {
        if (solrClient == null) {
          solrClient = new CloudSolrClient.Builder().withZkHost(getZkHosts(globalConfig)).build();
        }
      }
    }
    return solrClient;
  }

  /**
   * Closes the SolrClient connection and releases the reference.
   */
  public static void close() {
    synchronized (SolrClientFactory.class) {
      if (solrClient != null) {
        try {
          solrClient.close();
        } catch (IOException e) {
          LOG.error(e.getMessage(), e);
        } finally {
          solrClient = null;
        }
      }
    }
  }

  /**
   * Retrieves zookeeper hosts from the global config and formats them for CloudSolrClient instantiation.
   * @param globalConfig Global config
   * @return A list of properly formatted zookeeper servers
   */
  protected static List<String> getZkHosts(Map<String, Object> globalConfig) {
    return Splitter.on(',').trimResults()
            .splitToList((String) globalConfig.getOrDefault(SOLR_ZOOKEEPER, ""));
  }
}
