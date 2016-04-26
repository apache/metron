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
package org.apache.metron.test.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public abstract class BaseBoltTest {
  
  @Mock
  protected TopologyContext topologyContext;

  @Mock
  protected OutputCollector outputCollector;

  @Mock
  protected Tuple tuple;

  @Mock
  protected OutputFieldsDeclarer declarer;

  @Mock
  protected CuratorFramework client;

  @Mock
  protected TreeCache cache;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  protected class FieldsMatcher extends ArgumentMatcher<Fields> {

    private List<String> expectedFields;

    public FieldsMatcher(String... fields) {
      this.expectedFields = Arrays.asList(fields);
    }

    @Override
    public boolean matches(Object o) {
      Fields fields = (Fields) o;
      return expectedFields.equals(fields.toList());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(String.format("[%s]", Joiner.on(",").join(expectedFields)));
    }

  }

  public void removeTimingFields(JSONObject message) {
    ImmutableSet keys = ImmutableSet.copyOf(message.keySet());
    for (Object key : keys) {
      if (key.toString().endsWith(".ts")) {
        message.remove(key);
      }
    }
  }
}
