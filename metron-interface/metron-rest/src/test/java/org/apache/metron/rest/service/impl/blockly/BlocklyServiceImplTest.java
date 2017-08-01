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
package org.apache.metron.rest.service.impl.blockly;

import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.config.HadoopConfig;
import org.apache.metron.rest.service.impl.blockly.BlocklyServiceImpl;
import org.apache.metron.rest.service.impl.HdfsServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={BlocklyServiceImplTest.BlocklyTestContextConfiguration.class})
@ActiveProfiles(TEST_PROFILE)
public class BlocklyServiceImplTest {

  @Configuration
  @Profile(TEST_PROFILE)
  static class BlocklyTestContextConfiguration {

    @Bean
    public CuratorFramework curatorFramework() {
      return mock(CuratorFramework.class);
    }
  }

  @Autowired
  private CuratorFramework client;

  @Test
  public void test() throws IOException {
    String statement = "IS_EMAIL(sensor_type) && sensor_type == 'yaf'";
    //String statement = "foo in [ TO_LOWER('CASEY'), 'david' ]";
    //String statement = "STATS_PERCENTILE( STATS_MERGE( PROFILE_GET('host-in-degree', ip_src_addr, 1, 'HOURS')), 95)";
    //String statement = "not(ENDS_WITH(domain_without_subdomains, '.com') or ENDS_WITH(domain_without_subdomains, '.net'))";
    //String statement = "(1.1 < 2.2 ? 'one' : 'two') == 'two'";
    //String statement = "sensor_type == null or true";
    //String statement = "EXISTS(sensor_type)";
    //String statement = "application not in ['test1', 'test2', 'test3']";
    //String statement = "MAP_EXISTS('test',{'test' : application, 'field' : 'value'})";
    BlocklyServiceImpl blocklyService = new BlocklyServiceImpl();
    try {
      System.out.println(blocklyService.statementToXml(statement));
    } catch (RestException e) {
      e.printStackTrace();
    }
  }
}
