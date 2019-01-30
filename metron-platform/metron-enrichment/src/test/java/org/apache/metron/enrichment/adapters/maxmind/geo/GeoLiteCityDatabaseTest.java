/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.enrichment.adapters.maxmind.geo;

import static org.apache.metron.enrichment.adapters.maxmind.MaxMindDatabase.EXTENSION_MMDB_GZ;
import static org.apache.metron.enrichment.adapters.maxmind.MaxMindDatabase.EXTENSION_TAR_GZ;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GeoLiteCityDatabaseTest {

  private static Context context;
  private static File geoHdfsFile;
  private static File geoHdfsFileTarGz;
  private static File geoHdfsFile_update;
  private static final String IP_WITH_DMA = "81.2.69.192";
  private static final String IP_NO_DMA = "216.160.83.56";
  private static final String GEO_CITY = "GeoLite2-City";
  private static final String GEO_CITY_FILE_NAME = GEO_CITY + EXTENSION_MMDB_GZ;
  private static final String GEO_CITY_COPY_FILE_NAME = GEO_CITY + "-2" + EXTENSION_MMDB_GZ;

  /**
   * {
   * "locID":"5803556",
   * "country":"US",
   * "city":"Milton",
   * "postalCode":"98354",
   * "latitude":"47.2513",
   * "longitude":"-122.3149",
   * "dmaCode":"819",
   * "location_point":"47.2513,-122.3149"
   * }
   */
  @Multiline
  private static String expectedNoDmaMessageString;
  private static JSONObject expectedNoDmaMessage;

  /**
   * {
   * "locID":"2643743",
   * "country":"GB",
   * "city":"London",
   * "postalCode":"",
   * "latitude":"51.5142",
   * "longitude":"-0.0931",
   * "dmaCode":"",
   * "location_point":"51.5142,-0.0931"
   * }
   */
  @Multiline
  private static String expectedDmaMessageString;
  private static JSONObject expectedDmaMessage;


  /**
   * {
   * "locID":"2640894",
   * "country":"GB",
   * "city":"Orpington",
   * "postalCode":"BR6",
   * "latitude":"51.3581",
   * "longitude":"0.1277",
   * "dmaCode":"",
   * "location_point":"51.3581,0.1277"
   * }
   */
  @Multiline
  private static String expectedMessageStringTarGz;
  private static JSONObject expectedMessageTarGz;

  private static FileSystem fs;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupOnce() throws ParseException, IOException {
    JSONParser jsonParser = new JSONParser();
    expectedNoDmaMessage = (JSONObject) jsonParser.parse(expectedNoDmaMessageString);
    expectedDmaMessage = (JSONObject) jsonParser.parse(expectedDmaMessageString);
    expectedMessageTarGz = (JSONObject) jsonParser.parse(expectedMessageStringTarGz);

    String baseDir = UnitTestHelper.findDir("GeoLite");
    geoHdfsFile = new File(new File(baseDir), GEO_CITY_FILE_NAME);
    geoHdfsFile_update = new File(new File(baseDir), GEO_CITY_COPY_FILE_NAME);
    FileUtils.copyFile(geoHdfsFile, geoHdfsFile_update);
    geoHdfsFileTarGz = new File(new File(baseDir), GEO_CITY + EXTENSION_TAR_GZ);

    Configuration config = new Configuration();
    fs = FileSystem.get(config);
  }

  @AfterClass
  public static void tearDown() {
    FileUtils.deleteQuietly(geoHdfsFile_update);
  }

  @Before
  public void setup() throws Exception {
    testFolder.create();
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG
            , () -> ImmutableMap.of(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath())
    )
            .build();
  }

  @Test
  public void testGetLocal() {
    GeoLiteCityDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get("192.168.0.1");
    Assert.assertFalse("Local address result should be empty", result.isPresent());
  }

  @Test
  public void testExternalAddressNotFound() {
    GeoLiteCityDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    // the range 203.0.113.0/24 is assigned as "TEST-NET-3" and should never be locatable
    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get("203.0.113.1");
    Assert.assertFalse("External address not found", result.isPresent());
  }

  @Test
  public void testGetRemoteWithDma() {
    GeoLiteCityDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(IP_WITH_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedDmaMessage, result.get());
  }

  @Test
  public void testGetRemoteWithTarGzFile() {
    GeoLiteCityDatabase.INSTANCE.update(geoHdfsFileTarGz.getAbsolutePath());

    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(IP_WITH_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedMessageTarGz, result.get());
  }

  @Test
  public void testGetRemoteNoDma() {
    GeoLiteCityDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }

  @Test
  public void testMultipleUpdates() {
    GeoLiteCityDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());
    GeoLiteCityDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }

  @Test
  public void testUpdateIfNecessary() {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath());
    GeoLiteCityDatabase.INSTANCE.updateIfNecessary(globalConfig);

    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }


  @Test
  public void testMultipleUpdateIfNecessary() {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath());
    GeoLiteCityDatabase.INSTANCE.updateIfNecessary(globalConfig);
    GeoLiteCityDatabase.INSTANCE.updateIfNecessary(globalConfig);

    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }

  @Test
  public void testDifferingUpdateIfNecessary() {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath());
    GeoLiteCityDatabase.INSTANCE.updateIfNecessary(globalConfig);
    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());

    globalConfig.put(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile_update.getAbsolutePath());
    GeoLiteCityDatabase.INSTANCE.updateIfNecessary(globalConfig);
    result = GeoLiteCityDatabase.INSTANCE.get(IP_NO_DMA);

    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }

  @Test
  public void testFallbackUnnecessary() {
    String fakeFile = "fakefile.geolitecitydbtest";
    Map<String, Object> globalConfig = Collections.singletonMap(GeoLiteCityDatabase.GEO_HDFS_FILE, fakeFile);
    Assert.assertEquals(
        GeoLiteCityDatabase.INSTANCE.determineHdfsDirWithFallback(globalConfig, fakeFile, ""),
        fakeFile);
  }

  @Test
  public void testFallbackUnncessaryAlreadyDefault() {
    String defaultFile = GeoLiteCityDatabase.GEO_HDFS_FILE_DEFAULT;
    Map<String, Object> globalConfig = Collections.singletonMap(GeoLiteCityDatabase.GEO_HDFS_FILE, defaultFile);
    Assert.assertEquals(
        GeoLiteCityDatabase.INSTANCE.determineHdfsDirWithFallback(globalConfig, defaultFile, ""),
        defaultFile);
  }

  @Test
  public void testFallbackToDefault() {
    String defaultFile = GeoLiteCityDatabase.GEO_HDFS_FILE_DEFAULT;
    Assert.assertEquals(GeoLiteCityDatabase.INSTANCE.determineHdfsDirWithFallback(Collections.emptyMap(), defaultFile, "fallback"), defaultFile);
  }

  @Test
  public void testFallbackToOldDefault() throws IOException {
    String fakeFile = "fakefile.geolitecitydbtest";
    File file = File.createTempFile( this.getClass().getSimpleName(), "testfile");
    file.deleteOnExit();
    String fileName = file.getAbsolutePath();
    Assert.assertEquals(GeoLiteCityDatabase.INSTANCE.determineHdfsDirWithFallback(Collections.emptyMap(), fakeFile, fileName), fileName);
  }
}
