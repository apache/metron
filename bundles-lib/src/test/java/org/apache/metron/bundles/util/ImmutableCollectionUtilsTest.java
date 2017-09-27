/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.bundles.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class ImmutableCollectionUtilsTest {

  @Test
  public void immutableMapOfSets() throws Exception {
    Map<String,Set<String>> map = new HashMap<>();
    map.put("one",new HashSet<String>(){{ add("a");}});

    Map<String,Set<String>> immutableMap = ImmutableCollectionUtils.immutableMapOfSets(map);
    Assert.assertNotNull(immutableMap);
    boolean thrown = false;
    try {
      immutableMap.put("test", new HashSet<String>());
    } catch(UnsupportedOperationException e){
      thrown = true;
    }
    Assert.assertTrue(thrown);
    thrown = false;

    try {
      immutableMap.get("one").add("b");
    } catch(UnsupportedOperationException e) {
      thrown = true;
    }

    Assert.assertTrue(thrown);

  }

  @Test
  public void immutableMapOfLists() throws Exception {
    Map<String,List<String>> map = new HashMap<>();
    map.put("one", Arrays.asList("a"));

    Map<String,List<String>> immutableMap = ImmutableCollectionUtils.immutableMapOfLists(map);
    Assert.assertNotNull(immutableMap);
    boolean thrown = false;
    try {
      immutableMap.put("test", new LinkedList<>());
    } catch(UnsupportedOperationException e){
      thrown = true;
    }
    Assert.assertTrue(thrown);
    thrown = false;

    try {
      immutableMap.get("one").add("b");
    } catch(UnsupportedOperationException e) {
      thrown = true;
    }

    Assert.assertTrue(thrown);
  }

}