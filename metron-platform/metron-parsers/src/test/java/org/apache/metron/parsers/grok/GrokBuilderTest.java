/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.parsers.grok;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import oi.thekraken.grok.api.Grok;
import org.apache.metron.test.utils.ResourceCopier;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GrokBuilderTest {

  @BeforeClass
  public static void before() throws Exception {
    ResourceCopier.copyResources(Paths.get("./src/test/resources/"), Paths.get("./target"));
    ResourceCopier.copyResources(Paths.get("./src/test/resources/"), Paths.get("./target/hdfs"));
  }

  @Test
  public void withParserConfiguration() throws Exception {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("grokPath", "/patterns/test");
    parserConfig.put("patternLabel", "YAF_TIME_FORMAT");
    Map<String, Object> globalConfig = ImmutableMap.of("metron.apps.hdfs.dir", "./target/hdfs");
    parserConfig.put("globalConfig", globalConfig);

    Grok grok = new GrokBuilder().withParserConfiguration(parserConfig).withLoadCommon(false)
        .build();
    Assert.assertNotNull(grok);
  }

  @Test
  public void withOutParserConfiguration() throws Exception {
    String grokPath = "/patterns/test";
    String patternLabel = "YAF_TIME_FORMAT";

    Grok grok = new GrokBuilder().withPatternLabel(patternLabel).withGrokPath(grokPath)
        .withLoadCommon(false).build();
    Assert.assertNotNull(grok);
  }

  @Test
  public void withReader() throws Exception {
    InputStreamReader reader = new InputStreamReader(
        new FileInputStream(new File("./target/patterns/test")));
    Grok grok = new GrokBuilder().withReader(reader).withLoadCommon(false).build();
    Assert.assertNotNull(grok);
  }

  @Test
  public void withPatternsCommonDir() throws Exception {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("grokPath", "/patterns/test");
    parserConfig.put("patternLabel", "YAF_TIME_FORMAT");
    Map<String, Object> globalConfig = ImmutableMap.of("metron.apps.hdfs.dir", "./target/hdfs");
    parserConfig.put("globalConfig", globalConfig);

    Grok grok = new GrokBuilder().withParserConfiguration(parserConfig)
        .withPatternsCommonDir("/otherPatterns/common").build();
    Assert.assertNotNull(grok);
  }

}