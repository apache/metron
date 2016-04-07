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

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.metron.enrichment.adapters.jdbc.JdbcAdapter;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.sql.ResultSet;

public class GeoAdapter extends JdbcAdapter {

  private InetAddressValidator ipvalidator = new InetAddressValidator();

  @Override
  public void logAccess(CacheKey value) {

  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject enrich(CacheKey value) {
    JSONObject enriched = new JSONObject();
    try {
      InetAddress addr = InetAddress.getByName(value.getValue());
      if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
              || addr.isSiteLocalAddress() || addr.isMulticastAddress()
              || !ipvalidator.isValidInet4Address(value.getValue())) {
        return new JSONObject();
      }
      String locidQuery = "select IPTOLOCID(\"" + value
              + "\") as ANS";
      ResultSet resultSet = statement.executeQuery(locidQuery);
      String locid = null;
      if (resultSet.next()) {
        locid = resultSet.getString("ANS");
      }
      resultSet.close();
      if (locid == null) return new JSONObject();
      String geoQuery = "select * from location where locID = " + locid;
      resultSet = statement.executeQuery(geoQuery);
      if (resultSet.next()) {
        enriched.put("locID", resultSet.getString("locID"));
        enriched.put("country", resultSet.getString("country"));
        enriched.put("city", resultSet.getString("city"));
        enriched.put("postalCode", resultSet.getString("postalCode"));
        enriched.put("latitude", resultSet.getString("latitude"));
        enriched.put("longitude", resultSet.getString("longitude"));
        enriched.put("dmaCode", resultSet.getString("dmaCode"));
        enriched.put("location_point", enriched.get("longitude") + "," + enriched.get("latitude"));
      }
      resultSet.close();
    } catch (Exception e) {
      _LOG.error("Enrichment failure: " + e.getMessage(), e);
      return new JSONObject();
    }
    return enriched;
  }
}
