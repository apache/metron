/*
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

package org.apache.metron.common.field;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the {@link FieldNameConverters} class.
 */
public class FieldNameConvertersTest {

  private WriterConfiguration createConfig(String writer, String sensor, String json) throws Exception {

    IndexingConfigurations indexingConfig = new IndexingConfigurations();
    indexingConfig.updateSensorIndexingConfig(sensor, json.getBytes());
    return new IndexingWriterConfiguration(writer, indexingConfig);
  }

  /**
   * {
   *  "elasticsearch": {
   *
   *    "index": "theIndex",
   *    "batchSize": 100,
   *    "batchTimeout": 1000,
   *    "enabled": true,
   *    "fieldNameConverter": "DEDOT"
   *  }
   * }
   */
  @Multiline
  private static String jsonWithDedot;

  /**
   * The factory should be able to create a {@link DeDotFieldNameConverter}.
   */
  @Test
  public void testCreateDedot() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithDedot);

    // validate the converter created for 'bro'
    FieldNameConverter converter = FieldNameConverters.create(sensor, config);
    assertEquals(FieldNameConverters.DEDOT, converter);
  }

  /**
   * {
   *  "elasticsearch": {
   *
   *    "index": "theIndex",
   *    "batchSize": 100,
   *    "batchTimeout": 1000,
   *    "enabled": true,
   *    "fieldNameConverter": "NOOP"
   *  }
   * }
   */
  @Multiline
  private static String jsonWithNoop;

  /**
   * The factory should be able to create a {@link NoopFieldNameConverter}.
   */
  @Test
  public void testCreateNoop() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithNoop);

    // validate the converter created for 'bro'
    FieldNameConverter converter = FieldNameConverters.create(sensor, config);
    assertEquals(FieldNameConverters.NOOP, converter);
  }

  /**
   * {
   *  "elasticsearch": {
   *
   *    "index": "theIndex",
   *    "batchSize": 100,
   *    "batchTimeout": 1000,
   *    "enabled": true
   *  }
   * }
   */
  @Multiline
  private static String jsonWithNoConverter;

  /**
   * The factory should create a default {@link FieldNameConverter} if none has been defined
   * by the user in the writer configuration.
   */
  @Test
  public void testCreateDefault() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithNoConverter);

    // if none defined, should default to 'DEDOT'
    FieldNameConverter converter = FieldNameConverters.create(sensor, config);
    assertEquals(FieldNameConverters.DEDOT, converter);
  }

  /**
   * If the user changes the {@link FieldNameConverter} in the writer configuration, the new
   * {@link FieldNameConverter} should be used after the old one expires.
   */
  @Test
  public void testConfigChange() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";

    // no converter defined in config, should use 'DEDOT' converter
    WriterConfiguration config = createConfig(writer, sensor, jsonWithNoConverter);
    assertEquals(FieldNameConverters.DEDOT, FieldNameConverters.create(sensor, config));

    // an 'updated' config uses the 'NOOP' converter
    WriterConfiguration newConfig = createConfig(writer, sensor, jsonWithNoop);
    assertEquals(FieldNameConverters.NOOP, FieldNameConverters.create(sensor, newConfig));
  }

  /**
   * {
   *  "elasticsearch": {
   *
   *    "index": "theIndex",
   *    "batchSize": 100,
   *    "batchTimeout": 1000,
   *    "enabled": true,
   *    "fieldNameConverter": "INVALID"
   *  }
   * }
   */
  @Multiline
  private static String jsonWithInvalidConverter;

  /**
   * If an invalid field name converter is specified, it should fall-back to using the
   * default, noop converter.
   */
  @Test
  public void testCreateInvalid() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithInvalidConverter);

    // if invalid value defined, it should fall-back to using default 'DEDOT'
    FieldNameConverter converter = FieldNameConverters.create(sensor, config);
    assertEquals(FieldNameConverters.DEDOT, converter);
  }

  /**
   * {
   *  "elasticsearch": {
   *
   *    "index": "theIndex",
   *    "batchSize": 100,
   *    "batchTimeout": 1000,
   *    "enabled": true,
   *    "fieldNameConverter": ""
   *  }
   * }
   */
  @Multiline
  private static String jsonWithBlankConverter;

  /**
   * If the field name converter field is blank, it should fall-back to using the
   * default converter.
   */
  @Test
  public void testCreateBlank() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithInvalidConverter);

    // if invalid value defined, it should fall-back to using default 'DEDOT'
    FieldNameConverter converter = FieldNameConverters.create(sensor, config);
    assertEquals(FieldNameConverters.DEDOT, converter);
  }
}
