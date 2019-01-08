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
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.enrichment.adapters.maxmind.geo.GeoLiteCityDatabase;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeoEnrichmentFunctionsTest {
  private static Context context;
  private static File geoHdfsFile;

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
  private static String expectedMessageString;

  private static JSONObject expectedMessage;

  /**
   * {
   * "country":"US",
   * "city":"Milton",
   * "dmaCode":"819",
   * "location_point":"47.2513,-122.3149"
   * }
   */
  @Multiline
  private static String expectedSubsetString;

  private static JSONObject expectedSubsetMessage;

  @BeforeClass
  public static void setupOnce() throws ParseException {
    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);

    expectedSubsetMessage = (JSONObject) jsonParser.parse(expectedSubsetString);

    String baseDir = UnitTestHelper.findDir("GeoLite");
    geoHdfsFile = new File(new File(baseDir), "GeoLite2-City.mmdb.gz");
  }

  @Before
  public void setup() throws Exception {
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG
            , () -> ImmutableMap.of(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath())
    )
            .build();
  }

  public Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testMissingDb() {
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG
        , () -> ImmutableMap.of(GeoLiteCityDatabase.GEO_HDFS_FILE, "./fakefile.mmdb")
    ).build();
    String stellar = "GEO_GET()";
    try {
      run(stellar, ImmutableMap.of());
    } catch (Exception expected) {
      Assert.assertTrue(expected.getMessage().contains("File fakefile.mmdb does not exist"));
    }
  }

  @Test
  public void testMissingDbDuringUpdate() {
    String stellar = "GEO_GET()";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Null IP should return null", result);
    try {
      GeoLiteCityDatabase.INSTANCE.updateIfNecessary(
          Collections.singletonMap(GeoLiteCityDatabase.GEO_HDFS_FILE, "./fakefile.mmdb"));
    } catch (IllegalStateException e) {
      // ignore it, the file doesn't exist
    }
    // Should still continue to query the old database, instead of dying.
    result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Null IP should return null", result);
  }

  @Test
  public void testGetEmpty() {
    String stellar = "GEO_GET()";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Empty IP should return null", result);
  }

  @Test
  public void testGetNull() {
    String stellar = "GEO_GET(null)";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Null IP should return null", result);
  }

  @Test(expected = org.apache.metron.stellar.dsl.ParseException.class)
  public void testGetUndefined() {
    String stellar = "GEO_GET(undefined)";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Null IP should return null", result);
  }

  @Test
  public void testGetEmptyString() {
    String stellar = "GEO_GET('  ')";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertNull("Empty IP should return null", result);
  }

  @Test
  public void testGetLocal() {
    String stellar = "GEO_GET('192.168.0.1')";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Local IP should return empty map", new HashMap<String, String>(), result);
  }

  @Test
  public void testGetRemote() {
    String stellar = "GEO_GET('216.160.83.56')";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Remote IP should return result based on DB", expectedMessage, result);
  }

  @Test
  public void testGetRemoteSingleField() {
    String stellar = "GEO_GET('216.160.83.56', ['country'])";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Remote IP should return country result based on DB", "US", result);
  }

  @Test
  public void testGetRemoteMultipleFields() {
    String stellar = "GEO_GET('216.160.83.56', ['country', 'city', 'dmaCode', 'location_point'])";
    Object result = run(stellar, ImmutableMap.of());
    Assert.assertEquals("Remote IP should return country result based on DB", expectedSubsetMessage, result);
  }

  @Test(expected=org.apache.metron.stellar.dsl.ParseException.class)
  public void testGetTooManyParams() {
    String stellar = "GEO_GET('216.160.83.56', ['country', 'city', 'dmaCode', 'location_point'], 'garbage')";
    run(stellar, ImmutableMap.of());
  }
}
