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
package org.apache.metron.rest.config;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.metron.rest.MetronRestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;

@Configuration
public class HadoopConfig {

    private Environment environment;

    @Autowired
    public HadoopConfig(Environment environment) {
      this.environment = environment;
    }

    @Bean
    public org.apache.hadoop.conf.Configuration configuration() throws IOException {
        org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();
        configuration.set("fs.defaultFS", environment.getProperty(MetronRestConstants.HDFS_URL_SPRING_PROPERTY, MetronRestConstants.DEFAULT_HDFS_URL));
        if (environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)) {
            configuration.set("hadoop.security.authentication", "KERBEROS");
            UserGroupInformation.setConfiguration(configuration);
            String keyTabLocation = environment.getProperty(MetronRestConstants.KERBEROS_KEYTAB_SPRING_PROPERTY);
            String userPrincipal = environment.getProperty(MetronRestConstants.KERBEROS_PRINCIPLE_SPRING_PROPERTY);
            UserGroupInformation.loginUserFromKeytab(userPrincipal, keyTabLocation);
        }
        return configuration;
    }
}
