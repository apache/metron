/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.stellar.common.utils;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Serializer.
 */
public class SerDeUtilsTest {

  @Test
  public void testInteger() {
    final int expected = 2;
    byte[] raw = SerDeUtils.toBytes(expected);
    int actual = SerDeUtils.fromBytes(raw, Integer.class);
    assertEquals(expected, actual);
  }

  @Test
  public void testDouble() {
    final double expected = 2.0;
    byte[] raw = SerDeUtils.toBytes(expected);
    {
      double actual = SerDeUtils.fromBytes(raw, Double.class);
      assertEquals(expected, actual, 0.01);
    }
    {
      double actual = (double) SerDeUtils.fromBytes(raw, Object.class);
      assertEquals(expected, actual, 0.01);
    }
  }

  @Test
  public void testShort() {
    final short expected = 2;
    byte[] raw = SerDeUtils.toBytes(expected);
    {
      short actual = SerDeUtils.fromBytes(raw, Short.class);
      assertEquals(expected, actual);
    }
    {
      short actual = (short) SerDeUtils.fromBytes(raw, Object.class);
      assertEquals(expected, actual);
    }
  }

  @Test
  public void testLong() {
    final long expected = 2L;
    byte[] raw = SerDeUtils.toBytes(expected);
    {
      long actual = SerDeUtils.fromBytes(raw, Long.class);
      assertEquals(expected, actual);
    }
    {
      long actual = (Long) SerDeUtils.fromBytes(raw, Object.class);
      assertEquals(expected, actual);
    }
  }

  @Test
  public void testFloat() {
    final Float expected = 2.2F;
    byte[] raw = SerDeUtils.toBytes(expected);
    {
      float actual = SerDeUtils.fromBytes(raw, Float.class);
      assertEquals(expected, actual, 0.01);
    }
    {
      float actual = (float) SerDeUtils.fromBytes(raw, Object.class);
      assertEquals(expected, actual, 0.01);
    }
  }

  @Test
  public void testMap() {
    final Map<String, Object> expected = new HashMap<>();
    expected.put("foo", "bar");
    expected.put( "bar", 1.0);
    ;
    byte[] raw = SerDeUtils.toBytes(expected);
    Object actual = SerDeUtils.fromBytes(raw, Object.class);
    assertEquals(expected, actual);
  }

  @Test
  public void testList() {
    final List<String> expected = new ArrayList<String>();
    expected.add("foo");
    expected.add("bar");
    byte[] raw = SerDeUtils.toBytes(expected);
    Object actual = SerDeUtils.fromBytes(raw, Object.class);
    assertEquals(expected, actual);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBloomFilter() {
    final BloomFilter<Object> expected = new BloomFilter<>(new BloomFilter.DefaultSerializer<>(), 10000, 0.01);
    expected.add("foo");
    expected.add("bar");
    byte[] raw = SerDeUtils.toBytes(expected);
    BloomFilter<Object> actual = (BloomFilter) SerDeUtils.fromBytes(raw, Object.class);
    assertTrue(actual.mightContain("foo"));
    assertFalse(actual.mightContain("timothy"));
    assertEquals(expected, actual);
  }

  public static class ArbitraryPojo {
    private List<String> list = new ArrayList<>();
    private String string = "foo";
    private Double d = 1.0;
    private Map<String, String> map = new HashMap<>();
    private List<String> immutableList = ImmutableList.of("foo");

    public ArbitraryPojo() {
      list.add("foo");
      list.add("bar");
      map.put("key1", "value1");
      map.put("key2", "value2");

    }

    public List<String> getList() {
      return list;
    }

    public void setList(List<String> list) {
      this.list = list;
    }

    public String getString() {
      return string;
    }

    public void setString(String string) {
      this.string = string;
    }

    public Double getD() {
      return d;
    }

    public void setD(Double d) {
      this.d = d;
    }

    public Map<String, String> getMap() {
      return map;
    }

    public void setMap(Map<String, String> map) {
      this.map = map;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ArbitraryPojo that = (ArbitraryPojo) o;

      if (getList() != null ? !getList().equals(that.getList()) : that.getList() != null) return false;
      if (getString() != null ? !getString().equals(that.getString()) : that.getString() != null) return false;
      if (getD() != null ? !getD().equals(that.getD()) : that.getD() != null) return false;
      if (getMap() != null ? !getMap().equals(that.getMap()) : that.getMap() != null) return false;
      return immutableList != null ? immutableList.equals(that.immutableList) : that.immutableList == null;

    }

    @Override
    public int hashCode() {
      int result = getList() != null ? getList().hashCode() : 0;
      result = 31 * result + (getString() != null ? getString().hashCode() : 0);
      result = 31 * result + (getD() != null ? getD().hashCode() : 0);
      result = 31 * result + (getMap() != null ? getMap().hashCode() : 0);
      result = 31 * result + (immutableList != null ? immutableList.hashCode() : 0);
      return result;
    }
  }

  @Test
  public void testArbitraryPojo() {
    final ArbitraryPojo expected = new ArbitraryPojo();
    byte[] raw = SerDeUtils.toBytes(expected);
    Object actual = SerDeUtils.fromBytes(raw, Object.class);
    assertEquals(expected, actual);
  }
}
