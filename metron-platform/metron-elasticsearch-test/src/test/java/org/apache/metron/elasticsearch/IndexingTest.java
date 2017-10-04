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

package org.apache.metron.elasticsearch;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.metron.test.utils.UnitTestHelper;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class IndexingTest {

  private RestHighLevelClient client;
  private static Node node;
  private static final int httpPort = 9200;

  @BeforeClass
  public static void oneTimeSetup() throws IOException {
    File indexDir = UnitTestHelper.createTempDir(IndexingTest.class.toString());
    File logDir= new File(indexDir, "/logs");
    File dataDir= new File(indexDir, "/data");
    try {
      cleanDir(logDir);
      cleanDir(dataDir);

    } catch (IOException e) {
      throw new RuntimeException("Unable to clean log or data directories", e);
    }

    Settings.Builder settingsBuilder = Settings.builder()
        .put("cluster.name", "metron")
        .put("http.enabled", true)
//        .put("http.port", httpPort)
        .put("path.logs",logDir.getAbsolutePath())
        .put("path.data",dataDir.getAbsolutePath())
        .put("path.home", indexDir.getAbsoluteFile())
        .put("http.type", "http")
        .put("transport.type", "local");
//        .put("index.number_of_shards", 1)
//        .put("node.mode", "network")
//        .put("index.number_of_replicas", 1);
    node = new Node(settingsBuilder.build());
    wait(node, 60000);
  }

  private static void cleanDir(File dir) throws IOException {
    if(dir.exists()) {
      FileUtils.deleteDirectory(dir);
    }
    dir.mkdirs();
  }

  private static void wait(Node node, long timeoutMillis) {
    try {
      node.start();
      ClusterHealthResponse chr = (ClusterHealthResponse) node.client()
          .execute(ClusterHealthAction.INSTANCE, new ClusterHealthRequest().waitForStatus(
              ClusterHealthStatus.YELLOW).timeout(new TimeValue(timeoutMillis))).actionGet();
      if (chr != null && chr.isTimedOut()) {
        throw new RuntimeException("cluster state is " + chr.getStatus().name()
            + " and not " + ClusterHealthStatus.YELLOW.name()
            + ", from here on, everything will fail!");
      }
    } catch (NodeValidationException e) {
      throw new RuntimeException("node validation exception");
    }
  }

  @Before
  public void setup() {
//    RestClient restClient = RestClient.builder(new HttpHost("localhost", httpPort, "http")).build();
    RestClient restClient = RestClient.builder(new HttpHost("localhost", httpPort, "http")).build();
    client = new RestHighLevelClient(restClient);
  }

  @Test
  public void indexes_values() throws IOException {
    System.out.println(client.info().getClusterName());
  }

}
