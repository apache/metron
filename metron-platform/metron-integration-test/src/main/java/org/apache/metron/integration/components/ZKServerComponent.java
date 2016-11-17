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

import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.curator.test.TestingServer;
import java.util.Map;
public class ZKServerComponent implements InMemoryComponent{
    private TestingServer testZkServer;
    private String zookeeperUrl = null;
    private Map<String,String> properties = null;

    public String getConnectionString(){
        return this.zookeeperUrl;
    }

    @Override
    public void start() throws UnableToStartException {
        try {
            testZkServer = new TestingServer(true);
            zookeeperUrl = testZkServer.getConnectString();
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
        }catch(Exception e){}
    }
}
