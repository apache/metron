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
package org.apache.metron.rest.service.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.rest.model.SensorParserContext;
import org.apache.metron.rest.service.StellarService;
import org.apache.storm.shade.com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class StellarServiceImplTest {

  private StellarService stellarService;
  CuratorFramework curatorFramework;

  @BeforeEach
  public void setUp() throws Exception {
    curatorFramework = mock(CuratorFramework.class);
    stellarService = new StellarServiceImpl(curatorFramework);
  }

  @Test
  public void validateRulesShouldProperlyValidateRules() {
    List<String> rules = Arrays.asList("TO_LOWER(test)", "BAD_FUNCTION(test)");
    Map<String, Boolean> results = stellarService.validateRules(rules);
    assertEquals(2, results.size());
    assertEquals(true, results.get("TO_LOWER(test)"));
    assertEquals(false, results.get("BAD_FUNCTION(test)"));
  }

  @Test
  public void applyTransformationsShouldProperlyTransformData() {
    SensorParserConfig sensorParserConfig = new SensorParserConfig();
    FieldTransformer fieldTransformater = new FieldTransformer();
    fieldTransformater.setOutput("url_host");
    fieldTransformater.setTransformation("STELLAR");
    fieldTransformater.setConfig(new LinkedHashMap<String, Object>() {{
      put("url_host", "TO_LOWER(URL_TO_HOST(url))");
    }});
    sensorParserConfig.setFieldTransformations(ImmutableList.of(fieldTransformater));
    SensorParserContext sensorParserContext = new SensorParserContext();
    sensorParserContext.setSensorParserConfig(sensorParserConfig);
    sensorParserContext.setSampleData(new HashMap<String, Object>() {{
      put("url", "https://caseystella.com/blog");
    }});
    Map<String, Object> results = stellarService.applyTransformations(sensorParserContext);
    assertEquals(2, results.size());
    assertEquals("https://caseystella.com/blog", results.get("url"));
    assertEquals("caseystella.com", results.get("url_host"));
  }

  @Test
  public void getTransformationsShouldReturnTransformation() {
    assertTrue(stellarService.getTransformations().length > 0);
  }

  @Test
  public void getStellarFunctionsShouldReturnFunctions() {
    assertTrue(stellarService.getStellarFunctions().size() > 0);
  }

  @Test
  public void getSimpleStellarFunctionsShouldReturnFunctions() {
    assertEquals(1, stellarService.getSimpleStellarFunctions().stream()
            .filter(stellarFunctionDescription -> stellarFunctionDescription.getName().equals("TO_LOWER")).count());
  }

}
