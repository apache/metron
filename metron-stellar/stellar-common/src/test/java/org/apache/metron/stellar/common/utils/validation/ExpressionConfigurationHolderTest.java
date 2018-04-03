/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.utils.validation;

import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ExpressionConfigurationHolderTest {

  @Test
  @SuppressWarnings("unchecked")
  public void visitAndDiscover() throws Exception {
    List<String> discoveredPaths = new LinkedList<>();
    List<String> expectedPaths = new LinkedList(){{
      add("unit tests/test config/the field");
      add("unit tests/test config/the list of stellar strings/0");
      add("unit tests/test config/the list of stellar strings/1");
      add("unit tests/test config/plain old map/stellar_map_item");
      add("unit tests/test config/map two deep/level1/level2/nested stellar key");
      add("unit tests/test config/map depending on field type/stellar_key");
      add("unit tests/test config/child/child field");
      add("unit tests/test config/list of holders/0/child field");
      add("unit tests/test config/list of holders/1/child field");
    }};
    TestConfigObject testSubject = new TestConfigObject();
    ExpressionConfigurationHolder holder = new ExpressionConfigurationHolder("unit tests",
        "test config", testSubject);

    holder.discover();
    holder.visit(((path, statement) -> {
     discoveredPaths.add(path);
    }),((path, e) -> {
      Assert.assertTrue("Should not get errors", false);
    }));

    Assert.assertFalse(discoveredPaths.contains("unit tests/test config/map depending on field type but wrong/stellar_key"));

    Assert.assertEquals(discoveredPaths.size(),expectedPaths.size());
    expectedPaths.forEach((p) -> {
      if(!discoveredPaths.contains(p)) {
        Assert.assertTrue(String.format("discovered missing %s", p), false);
      }
    });

    discoveredPaths.removeAll(expectedPaths);

    Assert.assertTrue(discoveredPaths.isEmpty());
  }

}