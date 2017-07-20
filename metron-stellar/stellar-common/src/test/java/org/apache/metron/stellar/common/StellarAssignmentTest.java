/*
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
package org.apache.metron.stellar.common;

import com.google.common.collect.ImmutableList;
import org.apache.metron.stellar.common.StellarAssignment;
import org.junit.Assert;
import org.junit.Test;

public class StellarAssignmentTest {

  @Test
  public void testAssignment() {
    for(String statement : ImmutableList.of( "foo := bar + grok"
                                           , "foo   := bar + grok"
                                           , "foo := bar + grok   "
                                           )
       )
    {
      StellarAssignment assignment = StellarAssignment.from(statement);
      Assert.assertEquals("foo", assignment.getKey());
      Assert.assertEquals("foo", assignment.getVariable());
      Assert.assertEquals("bar + grok", assignment.getStatement());
      Assert.assertEquals("bar + grok", assignment.getValue());
    }
  }

  @Test
  public void testNonAssignment() {
    for(String statement : ImmutableList.of( "bar + grok"
                                           , "  bar + grok"
                                           , "bar + grok   "
    )
            )
    {
      StellarAssignment assignment = StellarAssignment.from(statement);
      Assert.assertNull( assignment.getKey());
      Assert.assertNull( assignment.getVariable());
      Assert.assertEquals("bar + grok", assignment.getStatement());
      Assert.assertEquals("bar + grok", assignment.getValue());
    }
  }

  @Test(expected=UnsupportedOperationException.class)
  public void testImmutability() {
    StellarAssignment assignment = StellarAssignment.from("foo := bar");
    assignment.setValue("myval");
  }
}
