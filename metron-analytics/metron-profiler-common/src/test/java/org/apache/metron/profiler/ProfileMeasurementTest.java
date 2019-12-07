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

package org.apache.metron.profiler;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileMeasurementTest {

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": {
   *      "profile": "2 + 2",
   *      "triage": {
   *        "eight": "4 + 4",
   *        "sixteen": "8 + 8"
   *      }
   *    }
   * }
   */
  @Multiline
  private String profile;
  private ProfileConfig definition;
  private ProfileMeasurement measurement;

  @BeforeEach
  public void setup() throws Exception {
    definition = ProfileConfig.fromJSON(profile);
    measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withDefinition(definition)
            .withPeriod(System.currentTimeMillis(), 10, TimeUnit.MINUTES)
            .withProfileValue(22)
            .withTriageValues(Collections.singletonMap("max", 200));
  }

  /**
   * Ensure that the {@link ProfileMeasurement} can undergo Kryo serialization which
   * occurs when the Profiler is running in Storm.
   */
  @Test
  public void testKryoSerialization() {
    assertNotNull(measurement);
    Kryo kryo = new Kryo();

    // serialize
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    Output output = new Output(byteStream);
    kryo.writeObject(output, measurement);

    // validate serialization
    byte[] bits = output.toBytes();
    assertNotNull(bits);

    // deserialize
    Input input = new Input(new ByteArrayInputStream(bits));
    ProfileMeasurement actual = kryo.readObject(input, ProfileMeasurement.class);

    // validate deserialization
    assertNotNull(actual);
    assertEquals(measurement, actual);
  }

  /**
   * Ensure that the {@link ProfileMeasurement} can undergo Java serialization, should a user
   * prefer that over Kryo serialization, which can occur when the Profiler is running
   * in Storm.
   */
  @Test
  public void testJavaSerialization() throws Exception {
    assertNotNull(measurement);

    // serialize using java
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    out.writeObject(measurement);

    // the serialized bits
    byte[] raw = bytes.toByteArray();
    assertTrue(raw.length > 0);

    // deserialize using java
    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(raw));
    Object actual = in.readObject();

    // ensure that the round-trip was successful
    assertEquals(measurement, actual);
  }
}
