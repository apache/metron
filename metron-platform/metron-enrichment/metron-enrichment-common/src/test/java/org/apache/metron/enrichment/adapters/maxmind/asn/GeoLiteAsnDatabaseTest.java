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
package org.apache.metron.enrichment.adapters.maxmind.asn;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.enrichment.adapters.maxmind.MaxMindDatabase.EXTENSION_TAR_GZ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EnableRuleMigrationSupport
public class GeoLiteAsnDatabaseTest {

  private static Context context;
  private static File asnHdfsFile;
  private static File asnHdfsFile_update;
  private static final String IP_ADDR = "8.8.4.0";
  private static final String GEO_ASN = "GeoLite2-ASN";
  private static final String GEO_ASN_FILE_NAME = GEO_ASN + EXTENSION_TAR_GZ;
  private static final String GEO_ASN_COPY_FILE_NAME = GEO_ASN + "-2" + EXTENSION_TAR_GZ;

  private static JSONObject expectedAsnMessage = new JSONObject();

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @BeforeAll
  @SuppressWarnings("unchecked")
  public static void setupOnce() throws IOException {
    // Construct this explicitly here, otherwise it'll be a Long instead of Integer.
    expectedAsnMessage.put("autonomous_system_organization", "Google LLC");
    expectedAsnMessage.put("autonomous_system_number", 15169);
    expectedAsnMessage.put("network", "8.8.4.0");

    String baseDir = UnitTestHelper.findDir("GeoLite");
    asnHdfsFile = new File(new File(baseDir), GEO_ASN_FILE_NAME);
    asnHdfsFile_update = new File(new File(baseDir), GEO_ASN_COPY_FILE_NAME);
    FileUtils.copyFile(asnHdfsFile, asnHdfsFile_update);
  }

  @AfterAll
  public static void tearDown() {
    FileUtils.deleteQuietly(asnHdfsFile_update);
  }

  @BeforeEach
  public void setup() throws Exception {
    testFolder.create();
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG,
        () -> ImmutableMap.of(GeoLiteAsnDatabase.ASN_HDFS_FILE, asnHdfsFile.getAbsolutePath())
    ).build();
  }

  @Test
  public void testGetLocal() {
    GeoLiteAsnDatabase.INSTANCE.update(asnHdfsFile.getAbsolutePath());

    Optional<Map<String, Object>> result = GeoLiteAsnDatabase.INSTANCE.get("192.168.0.1");
    assertFalse(result.isPresent(), "Local address result should be empty");
  }

  @Test
  public void testExternalAddressNotFound() {
    GeoLiteAsnDatabase.INSTANCE.update(asnHdfsFile.getAbsolutePath());

    // the range 203.0.113.0/24 is assigned as "TEST-NET-3" and should never be locatable
    Optional<Map<String, Object>> result = GeoLiteAsnDatabase.INSTANCE.get("203.0.113.1");
    assertFalse(result.isPresent(), "External address not found");
  }

  @Test
  public void testGetRemote() {
    GeoLiteAsnDatabase.INSTANCE.update(asnHdfsFile.getAbsolutePath());

    Optional<Map<String, Object>> result = GeoLiteAsnDatabase.INSTANCE.get(IP_ADDR);
    assertEquals(expectedAsnMessage,
        result.get(), "Remote Local IP should return result based on DB");
  }

  @Test
  public void testMultipleUpdates() {
    GeoLiteAsnDatabase.INSTANCE.update(asnHdfsFile.getAbsolutePath());
    GeoLiteAsnDatabase.INSTANCE.update(asnHdfsFile.getAbsolutePath());

    Optional<Map<String, Object>> result = GeoLiteAsnDatabase.INSTANCE.get(IP_ADDR);
    assertEquals(expectedAsnMessage, result.get(),
            "Remote Local IP should return result based on DB");
  }

  @Test
  public void testUpdateIfNecessary() {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteAsnDatabase.ASN_HDFS_FILE, asnHdfsFile.getAbsolutePath());
    GeoLiteAsnDatabase.INSTANCE.updateIfNecessary(globalConfig);

    Optional<Map<String, Object>> result = GeoLiteAsnDatabase.INSTANCE.get(IP_ADDR);
    assertEquals(expectedAsnMessage, result.get(),
            "Remote Local IP should return result based on DB");
  }

  @Test
  public void testMultipleUpdateIfNecessary() {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteAsnDatabase.ASN_HDFS_FILE, asnHdfsFile.getAbsolutePath());
    GeoLiteAsnDatabase.INSTANCE.updateIfNecessary(globalConfig);
    GeoLiteAsnDatabase.INSTANCE.updateIfNecessary(globalConfig);

    Optional<Map<String, Object>> result = GeoLiteAsnDatabase.INSTANCE.get(IP_ADDR);
    assertEquals(expectedAsnMessage, result.get(),
            "Remote Local IP should return result based on DB");
  }

  @Test
  public void testDifferingUpdateIfNecessary() {
    HashMap<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(GeoLiteAsnDatabase.ASN_HDFS_FILE, asnHdfsFile.getAbsolutePath());
    GeoLiteAsnDatabase.INSTANCE.updateIfNecessary(globalConfig);
    Optional<Map<String, Object>> result = GeoLiteAsnDatabase.INSTANCE.get(IP_ADDR);
    assertEquals(expectedAsnMessage, result.get(),
            "Remote Local IP should return result based on DB");

    globalConfig.put(GeoLiteAsnDatabase.ASN_HDFS_FILE, asnHdfsFile_update.getAbsolutePath());
    GeoLiteAsnDatabase.INSTANCE.updateIfNecessary(globalConfig);
    result = GeoLiteAsnDatabase.INSTANCE.get(IP_ADDR);

    assertEquals(expectedAsnMessage, result.get(),
            "Remote Local IP should return result based on DB");
  }
}
