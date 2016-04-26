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

package org.apache.metron.integration.components;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.metron.integration.InMemoryComponent;

import java.io.IOException;

public class MRComponent implements InMemoryComponent {
  private Configuration configuration;
  MiniDFSCluster cluster;
  private Path basePath;

  public MRComponent withBasePath(String path) {
    basePath = new Path(path);
    return this;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Path getBasePath() {
    return basePath;
  }

  @Override
  public void start()  {
    configuration = new Configuration();
    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA);
    configuration.set(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, "true");
    if(basePath == null) {
      throw new RuntimeException("Unable to start cluster: You must specify the basepath");
    }
    configuration.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, basePath.toString());
    try {
      cluster = new MiniDFSCluster.Builder(configuration)
                                  .build();
    } catch (IOException e) {
      throw new RuntimeException("Unable to start cluster", e);
    }
  }

  @Override
  public void stop() {
    cluster.shutdown();
  }
}
