/**
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
package org.apache.metron.enrichment.adapters.maxmind.geo;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.enrichment.adapters.geo.GeoAdapter;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GeoAdapterTest {
  private static final String IP = "216.160.83.56";

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

  private static GeoAdapter geo;
  private static File geoHdfsFile;

  @BeforeAll
  public static void setupOnce() throws ParseException {
    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);

    String baseDir = UnitTestHelper.findDir("GeoLite");
    geoHdfsFile = new File(new File(baseDir), "GeoLite2-City.mmdb.gz");

    geo = new GeoAdapter();
    geo.initializeAdapter(ImmutableMap.of(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath()));
  }

  @Test
  public void testEnrich() {
    JSONObject actualMessage = geo.enrich(new CacheKey("dummy", IP, null));

    assertNotNull(actualMessage.get("locID"));
    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  public void testEnrichNonString() {
    JSONObject actualMessage = geo.enrich(new CacheKey("dummy", 10L, null));
    assertEquals(new JSONObject(), actualMessage);
  }
}
