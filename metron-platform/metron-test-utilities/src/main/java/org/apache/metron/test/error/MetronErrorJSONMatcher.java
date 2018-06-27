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
package org.apache.metron.test.error;

import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.mockito.ArgumentMatcher;

public class MetronErrorJSONMatcher extends ArgumentMatcher<Values> {

  private JSONObject expected;

  public MetronErrorJSONMatcher(JSONObject expected) {
    this.expected = expected;
  }

  @Override
  public boolean matches(Object o) {
    Values values = (Values) o;
    JSONObject actual = (JSONObject) values.get(0);
    actual.remove("timestamp");
    expected.remove("timestamp");
    actual.remove("stack");
    expected.remove("stack");
    actual.remove("guid");
    expected.remove("guid");
    return actual.equals(expected);
  }
}
