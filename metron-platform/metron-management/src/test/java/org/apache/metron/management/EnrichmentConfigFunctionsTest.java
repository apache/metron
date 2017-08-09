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
package org.apache.metron.management;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.shell.StellarExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;

@RunWith(Parameterized.class)
public class EnrichmentConfigFunctionsTest {

  String configStr = emptyTransformationsConfig();
  Map<String, StellarExecutor.VariableResult> variables;
  Context context = null;
  String enrichmentType = null;
  String group = null;
  public EnrichmentConfigFunctionsTest(String enrichmentType, String group) {
    this.enrichmentType = enrichmentType;
    this.group = group;
  }

  public static String emptyTransformationsConfig() {
    SensorEnrichmentConfig config = new SensorEnrichmentConfig();
    try {
      return JSONUtils.INSTANCE.toJSON(config, true);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Parameterized.Parameters
  public static Collection<Object[]> types() {
    // each test will be run against these values for windowSize
    return Arrays.asList(new Object[][]{
      {"ENRICHMENT", "group"}
    , {"ENRICHMENT", null}
    , {"THREAT_INTEL", "group"}
    , {"THREAT_INTEL", null}
    });
  }



  @Before
  public void setup() {
    variables = ImmutableMap.of(
            "upper", new StellarExecutor.VariableResult("TO_UPPER('foo')", "FOO"),
            "lower", new StellarExecutor.VariableResult("TO_LOWER('FOO')", "foo")
    );

    context = new Context.Builder()
            .with(StellarExecutor.SHELL_VARIABLES, () -> variables)
            .build();
  }

  static Map<String, Object> toMap(String... k) {
    Map<String, Object> ret = new HashMap<>();
    for(int i = 0;i < k.length;i+=2) {
      ret.put(k[i], k[i+1]);
    }
    return ret;
  }
  private int size(Map<String, Object> stellarFunctions) {
    if(group == null) {
      return stellarFunctions.size();
    }
    else {
      return ((Map<String, Object>)stellarFunctions.getOrDefault(group, new HashMap<>())).size();
    }
  }
  private Object get(Map<String, Object> stellarFunctions, String key) {
    if(group == null) {
      return stellarFunctions.get(key);
    }
    else {
      return ((Map<String, Object>)stellarFunctions.get(group)).get(key);
    }
  }

  private EnrichmentConfig getEnrichmentConfig(String configStr) {
    SensorEnrichmentConfig sensorConfig = (SensorEnrichmentConfig) ENRICHMENT.deserialize(configStr);
    switch (enrichmentType) {
      case "ENRICHMENT":
        return sensorConfig.getEnrichment();
      case "THREAT_INTEL":
        return sensorConfig.getThreatIntel();
    }
    return null;
  }

  private static Map<String, Object> getStellarMappings(EnrichmentConfig config) {
    Map<String, Object> fieldMap = config.getFieldMap();
    if (fieldMap == null) {
      return new HashMap<>();
    }
    Map<String, Object> stellarMap = (Map<String, Object>) fieldMap.get("stellar");
    if (stellarMap == null) {
      return new HashMap<>();
    }
    return (Map<String, Object>) stellarMap.get("config");
  }

  private Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testAddEmpty() {

    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('upper'), group)"
            , toMap("config", configStr
                    , "type", enrichmentType
                    , "group", group
            )
    );
    Map<String, Object> stellarFunctions = getStellarMappings(getEnrichmentConfig(newConfig));
    Assert.assertEquals(1, size(stellarFunctions));
    Assert.assertEquals(variables.get("upper").getExpression(), get(stellarFunctions,"upper"));
  }

  @Test
  public void testAddHasExisting() {
    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('upper'), group)"
            ,toMap( "config", configStr
                  , "type", enrichmentType
                  , "group", group
                  )

            );
    newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('lower'), group)"
            , toMap("config",newConfig
                    , "type", enrichmentType
                    , "group", group
            )
    );
    Map<String, Object> stellarFunctions = getStellarMappings(getEnrichmentConfig(newConfig));
    Assert.assertEquals(2, size(stellarFunctions));
    Assert.assertEquals(variables.get("upper").getExpression(), get(stellarFunctions,"upper"));
    Assert.assertEquals(variables.get("lower").getExpression(), get(stellarFunctions,"lower"));
  }

  @Test
  public void testAddMalformed() {
    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('foo'), group)"
            , toMap("config", configStr
                    , "type", enrichmentType
                    , "group", group
            )
    );
    Map<String, Object> stellarFunctions = getStellarMappings(getEnrichmentConfig(newConfig));
    Assert.assertEquals(0, size(stellarFunctions));
  }

  @Test
  public void testAddDuplicate() {
    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('upper'), group)"
            , toMap("config", configStr
                    , "type", enrichmentType
                    , "group", group
            )
    );
    newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('upper'), group)"
            , toMap("config",newConfig
                    , "type", enrichmentType
                    , "group", group
            )
    );
    Map<String, Object> stellarFunctions = getStellarMappings(getEnrichmentConfig(newConfig));
    Assert.assertEquals(1, size(stellarFunctions));
    Assert.assertEquals(variables.get("upper").getExpression(), get(stellarFunctions,"upper"));
  }

  @Test
  public void testRemove() {
    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('upper', 'lower'), group)"
            , toMap("config", configStr
                    , "type", enrichmentType
                    , "group", group
            )
    );
    newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_REMOVE(config, type, ['upper'], group)"
            , toMap("config",newConfig
                    , "type", enrichmentType
                    , "group", group
            )
    );
    Map<String, Object> stellarFunctions = getStellarMappings(getEnrichmentConfig(newConfig));
    Assert.assertEquals(1, size(stellarFunctions));
    Assert.assertEquals(variables.get("lower").getExpression(), get(stellarFunctions,"lower"));
  }

  @Test
  public void testRemoveMultiple() {
    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('upper', 'lower'), group)"
            , toMap("config", configStr
                    , "type", enrichmentType
                    , "group", group
            )
    );
    newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_REMOVE(config, type, ['upper', 'lower'], group)"
            , toMap("config",newConfig
                    , "type", enrichmentType
                    , "group", group
            )
    );
    Map<String, Object> stellarFunctions = getStellarMappings(getEnrichmentConfig(newConfig));
    Assert.assertEquals(0, size(stellarFunctions));
  }

  @Test
  public void testRemoveMissing() {
    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('lower'), group)"
            , toMap("config", configStr
                    , "type", enrichmentType
                    , "group", group
            )
    );
    newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_REMOVE(config, type, ['upper'], group)"
            , toMap("config",newConfig
                    , "type", enrichmentType
                    , "group", group
            )
    );
    Map<String, Object> stellarFunctions = getStellarMappings(getEnrichmentConfig(newConfig));
    Assert.assertEquals(1, size(stellarFunctions));
    Assert.assertEquals(variables.get("lower").getExpression(), get(stellarFunctions,"lower"));
  }

  /**
╔═══════╤═══════╤═════════════════╗
║ Group │ Field │ Transformation  ║
╠═══════╪═══════╪═════════════════╣
║ group │ upper │ TO_UPPER('foo') ║
╚═══════╧═══════╧═════════════════╝
   */
  @Multiline
  static String testPrintExpectedWithGroup;
  /**
╔═══════════╤═══════╤═════════════════╗
║ Group     │ Field │ Transformation  ║
╠═══════════╪═══════╪═════════════════╣
║ (default) │ upper │ TO_UPPER('foo') ║
╚═══════════╧═══════╧═════════════════╝
   */
  @Multiline
  static String testPrintExpectedWithoutGroup;
  @Test
  public void testPrint() {
    String newConfig = (String) run(
            "ENRICHMENT_STELLAR_TRANSFORM_ADD(config, type, SHELL_VARS2MAP('upper'), group)"
            , toMap("config", configStr
                    , "type", enrichmentType
                    , "group", group
            )
    );
    String out = (String) run("ENRICHMENT_STELLAR_TRANSFORM_PRINT(config, type)"
            , toMap("config", newConfig
                   ,"type", enrichmentType
                  )
    );
    if(group == null) {
      Assert.assertEquals(testPrintExpectedWithoutGroup, out);
    }
    else {
      Assert.assertEquals(testPrintExpectedWithGroup, out);
    }
  }

  /**
╔═══════╤═══════╤════════════════╗
║ Group │ Field │ Transformation ║
╠═══════╧═══════╧════════════════╣
║ (empty)                        ║
╚════════════════════════════════╝
   */
  @Multiline
  static String testPrintEmptyExpected;

  @Test
  public void testPrintEmpty() {
    String out = (String) run("ENRICHMENT_STELLAR_TRANSFORM_PRINT(config, type)"
            , toMap("config", configStr
                   ,"type", enrichmentType
                  )
    );
    Assert.assertEquals(testPrintEmptyExpected, out);
  }

  @Test
  public void testPrintNull() {

    String out = (String) run("ENRICHMENT_STELLAR_TRANSFORM_PRINT(config, type)"
            , toMap("config", configStr ,"type", enrichmentType)
    );
    Assert.assertEquals(testPrintEmptyExpected, out);
  }


}
