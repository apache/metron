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
package org.apache.metron.enrichment.adapters.geo;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.mockito.Mockito.when;

public class GeoAdapterTest {


  private String ip = "72.163.4.161";


  /**
   * {
   * "locID":"1",
   * "country":"test country",
   * "city":"test city",
   * "postalCode":"test zip",
   * "latitude":"test latitude",
   * "longitude":"test longitude",
   * "dmaCode":"test dma",
   * "location_point":"test longitude,test latitude"
   * }
   */
  @Multiline
  private String expectedMessageString;

  private JSONObject expectedMessage;

  @Mock
  Statement statetment;
  @Mock
  ResultSet resultSet, resultSet1;


  @Before
  public void setup() throws Exception {
    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);
    MockitoAnnotations.initMocks(this);
    when(statetment.executeQuery("select IPTOLOCID(\"" + ip + "\") as ANS")).thenReturn(resultSet);
    when(statetment.executeQuery("select * from location where locID = 1")).thenReturn(resultSet1);
    when(resultSet.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);
    when(resultSet.getString("ANS")).thenReturn("1");
    when(resultSet1.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);
    when(resultSet1.getString("locID")).thenReturn("1");
    when(resultSet1.getString("country")).thenReturn("test country");
    when(resultSet1.getString("city")).thenReturn("test city");
    when(resultSet1.getString("postalCode")).thenReturn("test zip");
    when(resultSet1.getString("latitude")).thenReturn("test latitude");
    when(resultSet1.getString("longitude")).thenReturn("test longitude");
    when(resultSet1.getString("dmaCode")).thenReturn("test dma");
  }


  @Test
  public void testEnrich() throws Exception {
    GeoAdapter geo = new GeoAdapter() {
      @Override
      public boolean initializeAdapter() {
        return true;
      }
    };
    geo.setStatement(statetment);
    JSONObject actualMessage = geo.enrich(new CacheKey("dummy", ip, null));
    Assert.assertNotNull(actualMessage.get("locID"));
    Assert.assertEquals(expectedMessage, actualMessage);
  }

}

