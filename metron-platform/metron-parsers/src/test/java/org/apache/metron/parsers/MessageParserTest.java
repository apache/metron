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

package org.apache.metron.parsers;

import junit.framework.Assert;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MessageParserTest {
  @Test
  public void testNullable() throws Exception {
    MessageParser parser = new MessageParser() {
      @Override
      public void init() {

      }

      @Override
      public List parse(byte[] rawMessage) {
        return null;
      }

      @Override
      public boolean validate(Object message) {
        return false;
      }

      @Override
      public void configure(Map<String, Object> config) {

      }
    };
    Assert.assertNotNull(parser.parseOptional(null));
    Assert.assertFalse(parser.parseOptional(null).isPresent());
  }

  @Test
  public void testNotNullable() throws Exception {
    MessageParser parser = new MessageParser() {
      @Override
      public void init() {

      }

      @Override
      public List parse(byte[] rawMessage) {
        return new ArrayList<>();
      }

      @Override
      public boolean validate(Object message) {
        return false;
      }

      @Override
      public void configure(Map<String, Object> config) {

      }
    };
    Assert.assertNotNull(parser.parseOptional(null));
    Optional<List> ret = parser.parseOptional(null);
    Assert.assertTrue(ret.isPresent());
    Assert.assertEquals(0, ret.get().size());
  }
}
