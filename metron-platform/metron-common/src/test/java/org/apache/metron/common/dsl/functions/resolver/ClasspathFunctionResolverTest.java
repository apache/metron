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

package org.apache.metron.common.dsl.functions.resolver;

import com.google.common.collect.Lists;
import org.apache.metron.common.dsl.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

public class ClasspathFunctionResolverTest {

  private static List<String> expectedFunctions;

  @BeforeClass
  public static void setup() {

    // search the entire classpath for functions - provides a baseline to test against
    Properties config = new Properties();
    config.put(ClasspathFunctionResolver.STELLAR_SEARCH_INCLUDES_KEY, "");
    config.put(ClasspathFunctionResolver.STELLAR_SEARCH_EXCLUDES_KEY, "");

    // use a permissive regex that should not filter anything
    ClasspathFunctionResolver resolver = create(config);

    expectedFunctions = Lists.newArrayList(resolver.getFunctions());
  }

  /**
   * Create a function resolver to test.
   * @param config The configuration for Stellar.
   */
  public static ClasspathFunctionResolver create(Properties config) {
    ClasspathFunctionResolver resolver = new ClasspathFunctionResolver();

    Context context = new Context.Builder()
            .with(Context.Capabilities.STELLAR_CONFIG, () -> config)
            .build();
    resolver.initialize(context);

    return resolver;
  }

  @Test
  public void testInclude() {

    // setup - include all `org.apache.metron.*` functions
    Properties config = new Properties();
    config.put(ClasspathFunctionResolver.STELLAR_SEARCH_INCLUDES_KEY, "org.apache.metron.*");

    // execute
    ClasspathFunctionResolver resolver = create(config);
    List<String> actual = Lists.newArrayList(resolver.getFunctions());

    // validate - should have found all of the functions
    Assert.assertEquals(expectedFunctions, actual);
  }

  @Test
  public void testWithMultipleIncludes() {

    // setup - include all of the common and management functions, which is most of them
    Properties config = new Properties();
    config.put(ClasspathFunctionResolver.STELLAR_SEARCH_INCLUDES_KEY, "org.apache.metron.common.*, org.apache.metron.management.*");

    // execute
    ClasspathFunctionResolver resolver = create(config);
    List<String> actual = Lists.newArrayList(resolver.getFunctions());

    // validate - should have found all of the functions
    Assert.assertTrue(actual.size() > 0);
    Assert.assertTrue(actual.size() <= expectedFunctions.size());
  }

  @Test
  public void testExclude() {

    // setup - exclude all `org.apache.metron.*` functions
    Properties config = new Properties();
    config.put(ClasspathFunctionResolver.STELLAR_SEARCH_EXCLUDES_KEY, "org.apache.metron.*");

    // use a permissive regex that should not filter anything
    ClasspathFunctionResolver resolver = create(config);
    List<String> actual = Lists.newArrayList(resolver.getFunctions());

    // both should have resolved the same functions
    Assert.assertEquals(0, actual.size());
  }

}
