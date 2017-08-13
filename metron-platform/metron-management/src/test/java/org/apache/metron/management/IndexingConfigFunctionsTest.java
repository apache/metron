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

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.shell.StellarExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.common.configuration.ConfigurationType.INDEXING;
import static org.apache.metron.management.EnrichmentConfigFunctionsTest.toMap;

public class IndexingConfigFunctionsTest {

  Map<String, StellarExecutor.VariableResult> variables;
  Context context = null;

  private Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
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

  @Test
  public void testSetBatch() {
    String out = (String) run("INDEXING_SET_BATCH(config, 'hdfs', 10)"
                             , toMap("config", "{}")
    );
    Map<String, Object> config = (Map<String, Object>)INDEXING.deserialize(out);
    Assert.assertEquals(10, IndexingConfigurations.getBatchSize((Map<String, Object>) config.get("hdfs")));
  }

  @Test
  public void testSetBatchWithTimeout() {
    String out = (String) run("INDEXING_SET_BATCH(config, 'hdfs', 10, 2)"
                             , toMap("config", "{}")
    );
    Map<String, Object> config = (Map<String, Object>)INDEXING.deserialize(out);
    Assert.assertEquals(10, IndexingConfigurations.getBatchSize((Map<String, Object>) config.get("hdfs")));
    Assert.assertEquals(2,  IndexingConfigurations.getBatchTimeout((Map<String, Object>) config.get("hdfs")));
  }

  @Test(expected=IllegalStateException.class)
  public void testSetBatchBad() {
    Map<String,Object> variables = new HashMap<String,Object>(){{
      put("config",null);
    }};
    run("INDEXING_SET_BATCH(config, 'hdfs', 10)"
                             , variables
    );
  }

  @Test
  public void testSetEnabled() {
    String out = (String) run("INDEXING_SET_ENABLED(config, 'hdfs', true)"
                             , toMap("config", "{}")
    );
    Map<String, Object> config = (Map<String, Object>)INDEXING.deserialize(out);
    Assert.assertTrue(IndexingConfigurations.isEnabled((Map<String, Object>) config.get("hdfs")));
  }

  @Test(expected=IllegalStateException.class)
  public void testSetEnabledBad() {
    Map<String,Object> variables = new HashMap<String,Object>(){{
      put("config",null);
    }};
    run("INDEXING_SET_ENABLED(config, 'hdfs', 10)"
                             , variables
    );
  }

  @Test
  public void testSetIndex() {
    String out = (String) run("INDEXING_SET_INDEX(config, 'hdfs', 'foo')"
            , toMap("config", "{}")
    );
    Map<String, Object> config = (Map<String, Object>)INDEXING.deserialize(out);
    Assert.assertEquals("foo", IndexingConfigurations.getIndex((Map<String, Object>)config.get("hdfs"), null));
  }

  @Test(expected= IllegalStateException.class)
  public void testSetIndexBad() {
    Map<String,Object> variables = new HashMap<String,Object>(){{
      put("config",null);
    }};
    run("INDEXING_SET_INDEX(config, 'hdfs', NULL)"
            , variables
    );
  }
}
