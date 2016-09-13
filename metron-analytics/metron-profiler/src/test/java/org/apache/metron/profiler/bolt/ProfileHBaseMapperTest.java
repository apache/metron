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

package org.apache.metron.profiler.bolt;

import backtype.storm.tuple.Tuple;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ProfileHBaseMapper class.
 */
public class ProfileHBaseMapperTest {

  Tuple tuple;
  ProfileHBaseMapper mapper;
  ProfileMeasurement measurement;
  DefaultStellarExecutor executor;
  RowKeyBuilder rowKeyBuilder;

  @Before
  public void setup() {
    executor = new DefaultStellarExecutor();

    rowKeyBuilder = mock(RowKeyBuilder.class);

    mapper = new ProfileHBaseMapper();
    mapper.setExecutor(executor);
    mapper.setRowKeyBuilder(rowKeyBuilder);

    measurement = new ProfileMeasurement("profile", "entity", 20000, 4);
    measurement.setValue(22);

    // the tuple will contain the original message
    tuple = mock(Tuple.class);
    when(tuple.getValueByField(eq("measurement"))).thenReturn(measurement);
  }

  /**
   * The mapper should execute the 'groupBy' Stellar expressions and use that to generate
   * a row key.
   */
  @Test
  public void testExecuteGroupBy() throws Exception {

    // setup - expression that refers to the ProfileMeasurement.end
    measurement.setGroupBy(Arrays.asList("2 + 2"));

    // execute
    mapper.rowKey(tuple);

    // capture the ProfileMeasurement that should be emitted
    ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
    verify(rowKeyBuilder).rowKey(any(), arg.capture());

    // validate
    List<Object> actual = arg.getValue();
    Assert.assertEquals(4.0, actual.get(0));
  }

  /**
   * The mapper should execute each 'groupBy' Stellar expression and use that to generate
   * a row key.  There can be multiple.
   */
  @Test
  public void testExecuteMultipleGroupBys() throws Exception {

    // setup - expression that refers to the ProfileMeasurement.end
    measurement.setGroupBy(Arrays.asList("2 + 2", "4 + 4"));

    // execute
    mapper.rowKey(tuple);

    // capture the ProfileMeasurement that should be emitted
    ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
    verify(rowKeyBuilder).rowKey(any(), arg.capture());

    // validate
    List<Object> actual = arg.getValue();
    Assert.assertEquals(4.0, actual.get(0));
    Assert.assertEquals(8.0, actual.get(1));
  }
}