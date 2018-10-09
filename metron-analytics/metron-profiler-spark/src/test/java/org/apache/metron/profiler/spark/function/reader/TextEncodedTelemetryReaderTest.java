/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.metron.profiler.spark.function.reader;

import org.apache.metron.profiler.spark.reader.TelemetryReaders;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Properties;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;

/**
 * Tests the {@link org.apache.metron.profiler.spark.reader.TextEncodedTelemetryReader} class.
 */
public class TextEncodedTelemetryReaderTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  private static SparkSession spark;
  private Properties profilerProperties;
  private Properties readerProperties;

  @BeforeClass
  public static void setupSpark() {
    SparkConf conf = new SparkConf()
            .setMaster("local")
            .setAppName("BatchProfilerIntegrationTest")
            .set("spark.sql.shuffle.partitions", "8");
    spark = SparkSession
            .builder()
            .config(conf)
            .getOrCreate();
  }

  @AfterClass
  public static void tearDownSpark() {
    if(spark != null) {
      spark.close();
    }
  }

  @Before
  public void setup() {
    readerProperties = new Properties();
    profilerProperties = new Properties();
  }

  @Test
  public void testCSV() {
    // re-write the test data as a CSV with a header record
    String pathToCSV = tempFolder.getRoot().getAbsolutePath();
    spark.read()
            .format("text")
            .load("src/test/resources/telemetry.json")
            .as(Encoders.STRING())
            .write()
            .mode("overwrite")
            .option("header", "true")
            .format("csv")
            .save(pathToCSV);

    // tell the profiler to use the CSV input data
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), pathToCSV);
    profilerProperties.put(TELEMETRY_INPUT_FORMAT.getKey(), "csv");

    // set a reader property; tell the reader to expect a header
    readerProperties.put("header", "true");

    // there should be 100 valid JSON records
    Dataset<String> telemetry = TelemetryReaders.TEXT.read(spark, profilerProperties, readerProperties);
    Assert.assertEquals(100, telemetry.filter(new IsValidJSON()).count());
  }

  @Test
  public void testJSON() {
    // use the test telemetry that is stored as raw json
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");
    profilerProperties.put(TELEMETRY_INPUT_FORMAT.getKey(), "text");

    // set a reader property; tell the reader to expect a header
    readerProperties.put("header", "true");

    // there should be 100 valid JSON records
    Dataset<String> telemetry = TelemetryReaders.TEXT.read(spark, profilerProperties, readerProperties);
    Assert.assertEquals(100, telemetry.filter(new IsValidJSON()).count());
  }
}
