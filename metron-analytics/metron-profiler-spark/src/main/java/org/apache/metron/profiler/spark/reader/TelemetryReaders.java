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
   * Use a {@link TextEncodedTelemetryReader} by defining the property value as 'TEXT'.
   */
  TEXT(() -> new TextEncodedTelemetryReader()),

  /**
   * Use a {@link ColumnEncodedTelemetryReader} by defining the property value as 'COLUMNAR'.
   */
  COLUMNAR(() -> new ColumnEncodedTelemetryReader());

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
    LOG.debug("Creating telemetry reader: telemetryReader={}", propertyValue);
    TelemetryReader reader = null;
    try {
      TelemetryReaders strategy = TelemetryReaders.valueOf(propertyValue);
      reader = strategy.supplier.get();

    } catch(IllegalArgumentException e) {
      LOG.error("Unexpected telemetry reader: telemetryReader=" + propertyValue, e);
      throw e;
    }

    return reader;
  }

  @Override
  public Dataset<String> read(SparkSession spark, Properties profilerProps, Properties readerProps) {
    return supplier.get().read(spark, profilerProps, readerProps);
  }
}
