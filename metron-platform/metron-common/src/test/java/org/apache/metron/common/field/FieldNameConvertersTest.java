package org.apache.metron.common.field;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

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
   * The factory should be able to create a {@link NoopFieldNameConverter}.
   */
  @Test
  public void testCreateNoop() throws Exception {

    final String writer = "elasticsearch";
    final String sensor = "bro";
    WriterConfiguration config = createConfig(writer, sensor, jsonWithNoop);

    // validate the converter created for 'bro'
    FieldNameConverter converter = FieldNameConverters.create(sensor, config);
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
    FieldNameConverter converter = FieldNameConverters.create(sensor, config);
    assertTrue(converter instanceof DeDotFieldNameConverter);
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
    assertTrue(FieldNameConverters.create(sensor, config) instanceof DeDotFieldNameConverter);

    // an 'updated' config uses the 'NOOP' converter
    WriterConfiguration newConfig = createConfig(writer, sensor, jsonWithNoop);
    assertTrue(FieldNameConverters.create(sensor, newConfig) instanceof NoopFieldNameConverter);
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
    assertTrue(converter instanceof DeDotFieldNameConverter);
  }
}
