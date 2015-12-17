package com.opensoc.pcapservice;

import junit.framework.Assert;

import org.apache.hadoop.hbase.Cell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.opensoc.pcapservice.CellTimestampComparator;

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
