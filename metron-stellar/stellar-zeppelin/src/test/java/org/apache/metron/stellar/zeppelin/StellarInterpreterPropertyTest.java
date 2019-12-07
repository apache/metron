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
package org.apache.metron.stellar.zeppelin;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.apache.metron.stellar.zeppelin.StellarInterpreterProperty.ZOOKEEPER_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the StellarInterpreterProperty class.
 */
public class StellarInterpreterPropertyTest {

  /**
   * By defining the 'zookeeper.url' property a user is able to 'set' the Zookeeper URL.
   */
  @Test
  public void testGet() {

    // define the zookeeper URL property
    final String expected = "zookeeper:2181";
    Map<Object, Object> props = Collections.singletonMap("zookeeper.url", expected);

    // should be able to get the zookeeper URL property from the properties
    String actual = ZOOKEEPER_URL.get(props, String.class);
    assertEquals(expected, actual);
  }

  /**
   * The default value should be returned when the user does not defined a 'zookeeper.url'.
   */
  @Test
  public void testGetWhenPropertyNotDefined() {

    // the property is not defined
    Map<Object, Object> props = Collections.singletonMap("foo", "bar");
    String actual = ZOOKEEPER_URL.get(props, String.class);

    // expect to get the default value since its not defined
    String expected = ZOOKEEPER_URL.getDefault(String.class);
    assertEquals(expected, actual);
  }
}
