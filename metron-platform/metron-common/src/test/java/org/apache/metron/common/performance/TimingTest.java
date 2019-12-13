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

package org.apache.metron.common.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TimingTest {

  private Timing timing;

  @BeforeEach
  public void setup() {
    timing = new Timing();
  }

  @Test
  public void provides_monotonically_increasing_times_for_single_marker()
      throws InterruptedException {
    timing.mark("mark1");
    long tlast = 0;
    for (int i = 0; i < 10; i++) {
      tlast = timing.getElapsed("mark1");
      Thread.sleep(10);
      assertThat(timing.getElapsed("mark1") > tlast, equalTo(true));
    }
  }

  @Test
  public void provides_monotonically_increasing_times_for_multiple_markers()
      throws InterruptedException {
    timing.mark("mark1");
    timing.mark("mark2");
    long tlast1 = 0;
    long tlast2 = 0;
    for (int i = 0; i < 10; i++) {
      tlast1 = timing.getElapsed("mark1");
      tlast2 = timing.getElapsed("mark2");
      Thread.sleep(10);
      assertThat(timing.getElapsed("mark1") > tlast1, equalTo(true));
      assertThat(timing.getElapsed("mark2") > tlast2, equalTo(true));
    }
  }

  @Test
  public void elapsed_time_on_nonexistent_marker_is_zero() throws InterruptedException {
    timing.mark("mark1");
    long tlast1 = 0;
    for (int i = 0; i < 10; i++) {
      tlast1 = timing.getElapsed("mark1");
      Thread.sleep(10);
      assertThat(timing.getElapsed("mark1") > tlast1, equalTo(true));
      assertThat(timing.getElapsed("mark2"), equalTo(0L));
    }
  }

  @Test
  public void marking_again_resets_timing() throws InterruptedException {
    timing.mark("mark1");
    long tlast1 = 0;
    for (int i = 0; i < 5; i++) {
      Thread.sleep(10);
      tlast1 = timing.getElapsed("mark1");
      timing.mark("mark1");
      assertThat(timing.getElapsed("mark1") < tlast1, equalTo(true));
    }
  }

  @Test
  public void exists_checks_mark_existence() {
    timing.mark("mark1");
    assertThat(timing.exists("mark1"), equalTo(true));
    assertThat(timing.exists("mark2"), equalTo(false));
    assertThat(timing.exists("mark3"), equalTo(false));
  }

}
