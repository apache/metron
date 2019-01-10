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

package org.apache.metron.enrichment.stellar;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.metron.enrichment.adapters.maxmind.asn.GeoLiteAsnDatabase;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsnEnrichmentFunctionsTest {

  private static Context context;
  private static File asnHdfsFile;

  private static JSONObject expectedMessage = new JSONObject();
  private static JSONObject expectedSubsetMessage = new JSONObject();

  @BeforeClass
  @SuppressWarnings("unchecked")
  public static void setupOnce() {
    // Construct this explicitly here, otherwise it'll be a Long instead of Integer.
    expectedMessage.put("autonomous_system_organization", "Google LLC");
    expectedMessage.put("autonomous_system_number", 15169);
    expectedMessage.put("network", "8.8.4.0");

    expectedSubsetMessage.put("autonomous_system_organization", "Google LLC");
    expectedSubsetMessage.put("autonomous_system_number", 15169);

    String baseDir = UnitTestHelper.findDir("GeoLite");
    asnHdfsFile = new File(new File(baseDir), "GeoLite2-ASN.tar.gz");
  }

  @Before
  public void setup() throws Exception {
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG,
        () -> ImmutableMap.of(GeoLiteAsnDatabase.ASN_HDFS_FILE, asnHdfsFile.getAbsolutePath())
    ).build();
  }

  public Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
    return processor.parse(rule,
        new DefaultVariableResolver(variables::get, variables::containsKey),
        StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testMissingDb() {
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG,
        () -> ImmutableMap.of(GeoLiteAsnDatabase.ASN_HDFS_FILE, "./fakefile.mmdb")
    ).build();
    String stellar = "ASN_GET()";
    try {
      run(stellar, ImmutableMap.of());
    } catch (Exception expected) {
      Assert.assertTrue(expected.getMessage().contains("File fakefile.mmdb does not exist"));
    }
  }

  @Test
  public void testMissingDbDuringUpdate() {
    String stellar = "ASN_GET()";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Null IP should return null", result);
    try {
      GeoLiteAsnDatabase.INSTANCE.updateIfNecessary(
          Collections.singletonMap(GeoLiteAsnDatabase.ASN_HDFS_FILE, "./fakefile.mmdb"));
    } catch (IllegalStateException e) {
      // ignore it, the file doesn't exist
    }
    // Should still continue to query the old database, instead of dying.
    result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Null IP should return null", result);
  }

  @Test
  public void testGetEmpty() {
    String stellar = "ASN_GET()";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Empty IP should return null", result);
  }

  @Test
  public void testGetNull() {
    String stellar = "ASN_GET(null)";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Null IP should return null", result);
  }

  @Test(expected = org.apache.metron.stellar.dsl.ParseException.class)
  public void testGetUndefined() {
    String stellar = "ASN_GET(undefined)";
    run(stellar, ImmutableMap.of());
  }

  @Test
  public void testGetEmptyString() {
    String stellar = "ASN_GET('  ')";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Empty IP should return null", result);
  }

  @Test
  public void testGetLocal() {
    String stellar = "ASN_GET('192.168.0.1')";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Local IP should return empty map", new HashMap<String, String>(), result);
  }

  @Test
  public void testGetRemote() throws Exception {
    String stellar = "ASN_GET('8.8.4.0')";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Remote IP should return result based on DB", expectedMessage, result);
  }

  @Test
  public void testGetRemoteSingleField() throws Exception {
    String stellar = "ASN_GET('8.8.4.0', ['autonomous_system_organization'])";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Remote IP should return country result based on DB", "Google LLC", result);
  }

  @Test
  public void testGetRemoteSingleFieldInteger() throws Exception {
    String stellar = "ASN_GET('8.8.4.0', ['autonomous_system_number'])";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Remote IP should return country result based on DB", 15169, result);
  }

  @Test
  public void testGetRemoteMultipleFields() throws Exception {
    String stellar = "ASN_GET('8.8.4.0', ['autonomous_system_organization', 'autonomous_system_number'])";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Remote IP should return country result based on DB", expectedSubsetMessage,
        result);
  }

  @Test(expected = org.apache.metron.stellar.dsl.ParseException.class)
  public void testGetTooManyParams() throws Exception {
    String stellar = "ASN_GET('8.8.4.0', ['autonomous_system_organization', 'autonomous_system_number', 'network'], 'garbage')";
    run(stellar, ImmutableMap.of());
  }
}
