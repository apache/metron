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
package org.apache.metron.domain;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.metron.Constants;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurationTest {


    @Test
    public void testCanReadFromFile() throws Exception {

        Configuration configuration = new Configuration(Paths.get("./src/test/resources/config/"));
        configuration.update();

        checkResult(configuration);

    }

    @Test
    public void testCanReadFromZookeeper() throws Exception {

        CuratorFramework curatorFramework = mock(CuratorFramework.class);
        ExistsBuilder existsBuilder = mock(ExistsBuilder.class);
        GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
        GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);

        when(getDataBuilder.forPath(Constants.ZOOKEEPER_GLOBAL_ROOT)).thenReturn(mockGlobalData());
        when(curatorFramework.checkExists()).thenReturn(existsBuilder);
        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);
        when(getChildrenBuilder.forPath(anyString())).thenReturn(Collections.<String> emptyList());

        Configuration configuration = new Configuration(Paths.get("foo"));
        configuration.curatorFramework = curatorFramework;
        configuration.update();

        checkResult(configuration);
    }


    private byte[] mockGlobalData(){

        JSONObject global = new JSONObject();
        global.put("es.clustername", "metron");
        return global.toString().getBytes();

    }


    private void checkResult( Configuration configuration ){

        assertEquals("File contains 1 entry: ",1,configuration.getGlobalConfig().size());
        String esClustername = configuration.getGlobalConfig().get("es.clustername").toString();
        assertEquals("es.clustername should be \"metron\"","metron",esClustername);

    }
}

