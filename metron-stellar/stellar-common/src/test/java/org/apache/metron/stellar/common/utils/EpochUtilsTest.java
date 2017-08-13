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

package org.apache.metron.stellar.common.utils;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class EpochUtilsTest {

  @Test
  public void testSecondsToMillis() throws Exception {
    Long seconds = 1472131630L;
    Long value = EpochUtils.ensureEpochMillis(seconds);
    Assert.assertTrue(seconds.longValue() * 1000 == EpochUtils.ensureEpochMillis(seconds).longValue());
  }

  @Test
  public void testMillisStaysMillis() throws Exception {
    Long millis = 1472131630748L;
    Long value = EpochUtils.ensureEpochMillis(millis);
    Assert.assertTrue(millis.longValue() == EpochUtils.ensureEpochMillis(millis).longValue());
  }

  @Test
  public void testOtherLengthToMillis() throws Exception {
    Long meh = 1L;
    Assert.assertTrue(meh.longValue() == EpochUtils.ensureEpochMillis(meh).longValue());
  }



}