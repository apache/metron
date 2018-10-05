package org.apache.metron.profiler.spark.reader;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Allows a user to easily define the value of the property
 * {@link org.apache.metron.profiler.spark.BatchProfilerConfig#TELEMETRY_INPUT_READER}.
 */
public enum TelemetryReaders implements TelemetryReader {

  /**
   * Use a {@link TextEncodedTelemetryReader} by defining the property value as 'TEXT_READER'.
   */
  TEXT_READER(() -> new TextEncodedTelemetryReader()),

  /**
   * Use a {@link ColumnEncodedTelemetryReader} by defining the property value as 'COLUMN_READER'.
   */
  COLUMN_READER(() -> new ColumnEncodedTelemetryReader());

  static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Supplier<TelemetryReader> supplier;

  private TelemetryReaders(Supplier<TelemetryReader> supplier) {
    this.supplier = supplier;
  }

  /**
   * Returns a {@link TelemetryReader} based on a property value.
   *
   * @param propertyValue The property value.
   * @return A {@link TelemetryReader}
   * @throws IllegalArgumentException If the property value is invalid.
   */
  public static TelemetryReader create(String propertyValue) {
    LOG.debug("Using telemetry reader: {}", propertyValue);
    TelemetryReader reader = null;
    try {
      TelemetryReaders strategy = TelemetryReaders.valueOf(propertyValue);
      reader = strategy.supplier.get();

    } catch(IllegalArgumentException e) {
      LOG.error("Unexpected telemetry reader: " + propertyValue, e);
      throw e;
    }

    return reader;
  }

  /**
   * Uses the underlying {@link TelemetryReader} to read in the input telemetry.
   *
   * @param spark The spark session.
   * @param properties Additional properties for reading in the telemetry.
   * @return A {@link TelemetryReader}.
   */
  @Override
  public Dataset<String> read(SparkSession spark, Properties properties) {
    return supplier.get().read(spark, properties);
  }
}
