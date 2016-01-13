package com.opensoc.pcapservice;

import org.eclipse.jdt.internal.core.Assert;
import org.junit.Test;

import com.opensoc.pcapservice.ConfigurationUtil;
import com.opensoc.pcapservice.ConfigurationUtil.SizeUnit;

/**
 * The Class ConfigurationUtilTest.
 */
public class ConfigurationUtilTest {

  /**
   * Test_get max allowable result size in bytes.
   */
  @Test
  public void test_getMaxAllowableResultSizeInBytes() {
    long result = ConfigurationUtil.getMaxResultSize();
    Assert.isTrue(result == 62914560);
  }

  /**
   * Test_get max allowable results size unit.
   */
  @Test
  public void test_getMaxAllowableResultsSizeUnit() {
    SizeUnit result = ConfigurationUtil.getResultSizeUnit();
    Assert.isTrue(SizeUnit.MB == result);
  }

  /**
   * Test_get max row size in bytes.
   */
  @Test
  public void test_getMaxRowSizeInBytes() {
    long result = ConfigurationUtil.getMaxRowSize();
    Assert.isTrue(result == 71680);
  }

  /**
   * Test_get max row size unit.
   */
  @Test
  public void test_getMaxRowSizeUnit() {
    SizeUnit result = ConfigurationUtil.getRowSizeUnit();
    Assert.isTrue(SizeUnit.KB == result);
  }

}
