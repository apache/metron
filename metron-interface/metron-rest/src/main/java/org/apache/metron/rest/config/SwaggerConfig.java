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

import org.apache.metron.rest.MetronRestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.List;

import static org.apache.metron.rest.MetronRestConstants.KNOX_PROFILE;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  @Autowired
  private Environment environment;

  @Autowired
  ServletContext servletContext;

  @Bean
  public Docket api() {
    List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
    Docket docket = new Docket(DocumentationType.SWAGGER_2);
    if (activeProfiles.contains(KNOX_PROFILE)) {
      String knoxRoot = environment.getProperty(MetronRestConstants.KNOX_ROOT_SPRING_PROPERTY, String.class, "");
      docket = docket.pathProvider(new RelativePathProvider (servletContext) {
        @Override
        protected String applicationPath() {
          return knoxRoot;
        }

        @Override
        protected String getDocumentationPath() {
          return knoxRoot;
        }

        @Override
        public String getApplicationBasePath() {
          return knoxRoot;
        }

        @Override
        public String getOperationPath(String operationPath) {
          return knoxRoot + super.getOperationPath(operationPath);
        }
      });
    }
    return docket.select()
            .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
            .paths(PathSelectors.any())
            .build();
  }
}
