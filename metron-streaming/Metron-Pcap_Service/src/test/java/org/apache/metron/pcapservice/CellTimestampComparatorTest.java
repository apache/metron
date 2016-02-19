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
package org.apache.metron.pcapservice;

import junit.framework.Assert;

import org.apache.hadoop.hbase.Cell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.metron.pcapservice.CellTimestampComparator;

/**
 * The Class CellTimestampComparatorTest.
 */
public class CellTimestampComparatorTest {

  /**
   * Sets the up.
   * 
   * @throws Exception
   *           the exception
   */
  @Before
  public void setUp() throws Exception {
  }

  /**
   * Tear down.
   * 
   * @throws Exception
   *           the exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test_less.
   */
  @Test
  public void test_less() {
    // mocking
    Cell cell1 = Mockito.mock(Cell.class);
    Mockito.when(cell1.getTimestamp()).thenReturn(13945345808L);
    Cell cell2 = Mockito.mock(Cell.class);
    Mockito.when(cell2.getTimestamp()).thenReturn(13845345808L);

    CellTimestampComparator comparator = new CellTimestampComparator();

    // actual call and verify
    Assert.assertTrue(comparator.compare(cell2, cell1) == -1);

  }

  /**
   * Test_greater.
   */
  @Test
  public void test_greater() {
    // mocking
    Cell cell1 = Mockito.mock(Cell.class);
    Mockito.when(cell1.getTimestamp()).thenReturn(13745345808L);
    Cell cell2 = Mockito.mock(Cell.class);
    Mockito.when(cell2.getTimestamp()).thenReturn(13945345808L);

    CellTimestampComparator comparator = new CellTimestampComparator();

    // actual call and verify
    Assert.assertTrue(comparator.compare(cell2, cell1) == 1);

  }

  /**
   * Test_equal.
   */
  @Test
  public void test_equal() {
    // mocking
    Cell cell1 = Mockito.mock(Cell.class);
    Mockito.when(cell1.getTimestamp()).thenReturn(13945345808L);
    Cell cell2 = Mockito.mock(Cell.class);
    Mockito.when(cell2.getTimestamp()).thenReturn(13945345808L);

    CellTimestampComparator comparator = new CellTimestampComparator();

    // actual call and verify
    Assert.assertTrue(comparator.compare(cell2, cell1) == 0);

  }

}
