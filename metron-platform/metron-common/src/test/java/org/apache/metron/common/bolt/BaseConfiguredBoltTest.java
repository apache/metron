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
package org.apache.metron.common.bolt;

import org.apache.metron.test.bolt.BaseBoltTest;
import org.junit.Assert;

import java.util.HashSet;
import java.util.Set;

public class BaseConfiguredBoltTest extends BaseBoltTest {

  protected static Set<String> configsUpdated = new HashSet<>();

  protected void waitForConfigUpdate(final String expectedConfigUpdate) {
    waitForConfigUpdate(new HashSet<String>() {{ add(expectedConfigUpdate); }});
  }

  protected void waitForConfigUpdate(Set<String> expectedConfigUpdates) {
    int count = 0;
    while (!configsUpdated.equals(expectedConfigUpdates)) {
      if (count++ > 5) {
        Assert.fail("ConfiguredBolt was not updated in time");
        return;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
