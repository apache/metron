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
package org.apache.metron.common.system;

import org.junit.Test;
import org.mockito.Mockito;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ClockTest {

  @Test
  public void returns_system_time() throws Exception {
    Clock clock = new Clock();
    long t1 = clock.currentTimeMillis();
    Thread.sleep(50);
    long t2 = clock.currentTimeMillis();
    Thread.sleep(50);
    long t3 = clock.currentTimeMillis();
    assertThat("t3 should be greater", t3 > t2, equalTo(true));
    assertThat("t2 should be greater", t2 > t1, equalTo(true));
  }

  @Test
  public void formats_system_time_given_passed_format() throws Exception {
    Clock clock = Mockito.spy(Clock.class);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date date = sdf.parse("20160615183527162+0000");
    Mockito.when(clock.currentTimeMillis()).thenReturn(date.getTime());
    assertThat("time not right", clock.currentTimeFormatted("yyyyMMddHHmmssSSSZ"), equalTo("20160615183527162+0000"));
  }

}
