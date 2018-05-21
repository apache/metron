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
package org.apache.metron.elasticsearch.writer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.testing.FakeTicker;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.field.DeDotFieldNameConverter;
import org.apache.metron.common.field.FieldNameConverter;
import org.apache.metron.common.field.NoopFieldNameConverter;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link CachedFieldNameConverterFactory}.
 */
public class CachedFieldNameConverterFactoryTest {

  private CachedFieldNameConverterFactory factory;
  private Cache<String, FieldNameConverter> cache;
  private FakeTicker ticker;

  @Before
  public void setup() throws Exception {

    // allows us to advance time in the cache
    ticker = new FakeTicker();

    // a cache configured for testing
    cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .executor(Runnable::run)
            .ticker(ticker::read)
            .recordStats()
            .build();

    // the factory being tested
    factory = new CachedFieldNameConverterFactory(cache);
  }

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
   * The factory should be able to create the {@link FieldNameConverter}
   * that has been defined by the user.
   */
  @Test
  public void testCreateDedot() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithDedot);

    // validate the converter created for 'bro'
    FieldNameConverter converter = factory.create(sensor, config);
    assertTrue(converter instanceof DeDotFieldNameConverter);
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
   * The factory should be able to create the {@link FieldNameConverter}
   * that has been defined by the user.
   */
  @Test
  public void testCreateNoop() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithNoop);

    // validate the converter created for 'bro'
    FieldNameConverter converter = factory.create(sensor, config);
    assertTrue(converter instanceof NoopFieldNameConverter);
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
    FieldNameConverter converter = factory.create(sensor, config);
    assertTrue(converter instanceof DeDotFieldNameConverter);
  }

  /**
   * The factory should cache and reuse {@link FieldNameConverter} objects.
   */
  @Test
  public void testCacheUsage() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithNoConverter);

    // validate the converter created for 'bro'
    FieldNameConverter converter1 = factory.create(sensor, config);
    assertNotNull(converter1);
    assertEquals(1, cache.stats().requestCount());
    assertEquals(0, cache.stats().hitCount());
    assertEquals(1, cache.stats().missCount());

    // the converter should come from the cache on the next request
    FieldNameConverter converter2 = factory.create(sensor, config);
    assertNotNull(converter2);
    assertEquals(2, cache.stats().requestCount());
    assertEquals(1, cache.stats().hitCount());
    assertEquals(1, cache.stats().missCount());

    assertSame(converter1, converter2);
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
    assertTrue(factory.create(sensor, config) instanceof DeDotFieldNameConverter);

    // an 'updated' config uses the 'NOOP' converter
    WriterConfiguration newConfig = createConfig(writer, sensor, jsonWithNoop);

    // even though config has changed, the cache has not expired yet, still using 'DEDOT'
    assertTrue(factory.create(sensor, newConfig) instanceof DeDotFieldNameConverter);

    // advance 30 minutes
    ticker.advance(8, TimeUnit.MINUTES);

    // now the 'NOOP' converter should be used
    assertTrue(factory.create(sensor, newConfig) instanceof NoopFieldNameConverter);
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
    FieldNameConverter converter = factory.create(sensor, config);
    assertTrue(converter instanceof DeDotFieldNameConverter);
  }
}
