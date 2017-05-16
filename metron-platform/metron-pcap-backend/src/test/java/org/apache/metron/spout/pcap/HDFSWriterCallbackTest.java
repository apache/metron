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

package org.apache.metron.spout.pcap;

import org.apache.metron.spout.pcap.deserializer.KeyValueDeserializer;
import org.apache.storm.kafka.EmitContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

public class HDFSWriterCallbackTest {

  @Mock
  private EmitContext context;
  @Mock
  private HDFSWriterConfig config;
  @Mock
  private KeyValueDeserializer deserializer;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(config.getDeserializer()).thenReturn(deserializer);
  }

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void empty_or_null_key_throws_illegal_argument_exception() {
    HDFSWriterCallback callback = new HDFSWriterCallback().withConfig(config);
    List<Object> tuples = new ArrayList<>();
    tuples.add(null);
    tuples.add(null);

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Expected a key but none provided");

    callback.apply(tuples, context);
  }
}
