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

package org.apache.metron.profiler.storm;

import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfileResult;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.storm.tuple.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the ProfileHBaseMapper class.
 */
public class ProfileHBaseMapperTest {

  private Tuple tuple;
  private ProfileHBaseMapper mapper;
  private ProfileMeasurement measurement;
  private RowKeyBuilder rowKeyBuilder;
  private ProfileConfig profile;

  @Before
  public void setup() {
    rowKeyBuilder = mock(RowKeyBuilder.class);

    mapper = new ProfileHBaseMapper();
    mapper.setRowKeyBuilder(rowKeyBuilder);

    profile = new ProfileConfig("profile", "ip_src_addr", new ProfileResult("2 + 2"));

    measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withProfileValue(22)
            .withDefinition(profile);

    // the tuple will contain the original message
    tuple = mock(Tuple.class);
    when(tuple.getValueByField(eq("measurement"))).thenReturn(measurement);
  }

  /**
   * The mapper should return the expiration for a tuple based on the Profile definition.
   */
  @Test
  public void testExpires() throws Exception {
    final Long expiresDays = 30L;
    profile.setExpires(expiresDays);

    Optional<Long> actual = mapper.getTTL(tuple);
    Assert.assertTrue(actual.isPresent());
    Assert.assertEquals(expiresDays, (Long) TimeUnit.MILLISECONDS.toDays(actual.get()));
  }

  /**
   * The expiration field is optional within a Profile definition.
   */
  @Test
  public void testExpiresUndefined() throws Exception {
    // the TTL should not be defined
    Optional<Long> actual = mapper.getTTL(tuple);
    Assert.assertFalse(actual.isPresent());
  }
}
