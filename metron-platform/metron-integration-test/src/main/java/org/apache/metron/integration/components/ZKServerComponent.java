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

import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.curator.test.TestingServer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ZKServerComponent implements InMemoryComponent {
  public static final String ZOOKEEPER_PROPERTY = "zookeeper_quorum";
  private TestingServer testZkServer;
  private String zookeeperUrl = null;
  private Map<String,String> properties = null;
  private Optional<Consumer<ZKServerComponent>> postStartCallback = Optional.empty();
  public String getConnectionString()
  {
    return this.zookeeperUrl;
  }
  public ZKServerComponent withPostStartCallback(Consumer<ZKServerComponent> f) {
    postStartCallback = Optional.ofNullable(f);
    return this;
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      testZkServer = new TestingServer(true);
      zookeeperUrl = testZkServer.getConnectString();
      if(postStartCallback.isPresent()) {
        postStartCallback.get().accept(this);
      }
    }catch(Exception e){
      throw new UnableToStartException("Unable to start TestingServer",e);
    }
  }

  @Override
  public void stop() {
    try {
      if (testZkServer != null) {
        testZkServer.close();
      }
    }catch(Exception e){
      // Do nothing
    }
  }

  @Override
  public void reset() {
    if (testZkServer != null) {
      try {
        FileUtils.deleteDirectory(testZkServer.getTempDirectory());
      } catch (IOException e) {
        // Do nothing
      }
    }
  }
}
