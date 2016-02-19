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

import org.junit.Test;

import org.apache.metron.pcapservice.ConfigurationUtil;
import org.apache.metron.pcapservice.ConfigurationUtil.SizeUnit;
import org.springframework.util.Assert;

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
