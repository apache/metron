package com.apache.metron.pcapservice;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

import com.apache.metron.pcapservice.HBaseConfigurationUtil;

/**
 * The Class HBaseConfigurationUtilTest.
 */
public class HBaseConfigurationUtilTest {

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
   * Test_read.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void test_read() throws IOException {
    Configuration configuration = HBaseConfigurationUtil.read();
    Assert.isTrue(configuration != null, "Configuration must not be null");
    Assert.isTrue(configuration.get("hbase.client.retries.number").equals("1"),
        "value must be equal");
  }

}
