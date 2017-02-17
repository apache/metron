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
package org.apache.metron.enrichment.adapters.geo;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

public class GeoLiteDatabaseTest {
  private static Context context;
  private static File geoHdfsFile;
  private static File geoHdfsFile_update;
  private static final String IP_WITH_DMA = "81.2.69.192";
  private static final String IP_NO_DMA = "216.160.83.56";

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

  private static FileSystem fs;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupOnce() throws ParseException, IOException {
    JSONParser jsonParser = new JSONParser();
    expectedNoDmaMessage = (JSONObject) jsonParser.parse(expectedNoDmaMessageString);
    expectedDmaMessage = (JSONObject) jsonParser.parse(expectedDmaMessageString);

    String baseDir = UnitTestHelper.findDir("GeoLite");
    geoHdfsFile = new File(new File(baseDir), "GeoIP2-City-Test.mmdb.gz");
    geoHdfsFile_update = new File(new File(baseDir), "GeoIP2-City-Test-2.mmdb.gz");

    Configuration config = new Configuration();
    fs = FileSystem.get(config);
  }

  @Before
  public void setup() throws Exception {
    testFolder.create();
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG
            , () -> ImmutableMap.of(GeoLiteDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath())
    )
            .build();
  }

  @Test
  public void testGetLocal() throws Exception {
    GeoLiteDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get("192.168.0.1");
    Assert.assertEquals("Local IP should return empty map", new HashMap<String, String>(), result.get());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetRemoteWithDma() throws Exception {
    GeoLiteDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get(IP_WITH_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedDmaMessage, result.get());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetRemoteNoDma() throws Exception {
    GeoLiteDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMultipleUpdates() throws Exception {
    GeoLiteDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());
    GeoLiteDatabase.INSTANCE.update(geoHdfsFile.getAbsolutePath());

    Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateIfNecessary() throws Exception {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath());
    GeoLiteDatabase.INSTANCE.updateIfNecessary(globalConfig);

    Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }


  @Test
  @SuppressWarnings("unchecked")
  public void testMultipleUpdateIfNecessary() throws Exception {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath());
    GeoLiteDatabase.INSTANCE.updateIfNecessary(globalConfig);
    GeoLiteDatabase.INSTANCE.updateIfNecessary(globalConfig);

    Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDifferingUpdateIfNecessary() throws Exception {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath());
    GeoLiteDatabase.INSTANCE.updateIfNecessary(globalConfig);
    Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get(IP_NO_DMA);
    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());

    globalConfig.put(GeoLiteDatabase.GEO_HDFS_FILE, geoHdfsFile_update.getAbsolutePath());
    GeoLiteDatabase.INSTANCE.updateIfNecessary(globalConfig);
    result = GeoLiteDatabase.INSTANCE.get(IP_NO_DMA);

    Assert.assertEquals("Remote Local IP should return result based on DB", expectedNoDmaMessage, result.get());
  }
}
