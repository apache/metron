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

package org.apache.metron.integration;

import static org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver.Config.STELLAR_VFS_PATHS;
import static org.junit.jupiter.api.Assertions.assertTrue;


import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.integration.components.MRComponent;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StellarClasspathFunctionResolver {
  static MRComponent component;
  static Configuration configuration;
  @BeforeAll
  public static void setup() {
    component = new MRComponent().withBasePath("target");
    component.start();
    configuration = component.getConfiguration();

    try {
      FileSystem fs = FileSystem.newInstance(configuration);
      fs.mkdirs(new Path("/classpath-resources"));
      fs.copyFromLocalFile(new Path("src/test/classpath-resources/custom-1.0-SNAPSHOT.jar"), new Path("/classpath-resources"));
    } catch (IOException e) {
      throw new RuntimeException("Unable to start cluster", e);
    }
  }

  @AfterAll
  public static void teardown() {
    component.stop();
  }


  public static ClasspathFunctionResolver create(Properties config) {
    ClasspathFunctionResolver resolver = new ClasspathFunctionResolver();

    Context context = new Context.Builder()
            .with(Context.Capabilities.STELLAR_CONFIG, () -> config)
            .build();
    resolver.initialize(context);

    return resolver;
  }

  @Test
  public void test() throws Exception {
    Properties config = new Properties();
    config.put(STELLAR_VFS_PATHS.param(), configuration.get("fs.defaultFS") + "/classpath-resources/.*.jar");
    ClasspathFunctionResolver resolver = create(config);
    HashSet<String> functions = new HashSet<>(Lists.newArrayList(resolver.getFunctions()));
    assertTrue(functions.contains("NOW"));
  }

}
