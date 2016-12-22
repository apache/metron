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
package org.apache.metron.enrichment.integration.mock;

import com.google.common.base.Joiner;
import org.apache.metron.enrichment.adapters.geo.GeoAdapter;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;

//public class MockGeoAdapter implements EnrichmentAdapter<CacheKey>,
//        Serializable {
public class MockGeoAdapter extends GeoAdapter {
  public static final long START_IP = 0L;
  public static final long END_IP = Long.MAX_VALUE;


  public static final String DEFAULT_LOC_ID = "1";


  /*
  @Override
  protected boolean isLocalAddress(String ipStr, InetAddress addr) {
    return false;
  }

  @Override
  protected void refreshGeoData() throws IOException {
    if (map != null && db != null) {
      map.close();
      db.close();
    }

    db = DBMaker.memoryDB()
            .closeOnJvmShutdown()
            .fileDeleteAfterClose()
            .make();

    map = db.treeMap("dbName")
            .keySerializer(Serializer.LONG)
            .valueSerializer(Serializer.JAVA)
            .createOrOpen();

    map.put(START_IP,
            new GeoLocation(
                    END_IP, // endIp
                    DEFAULT_LOC_ID, // locId
                    DEFAULT_COUNTRY,
                    DEFAULT_CITY,
                    DEFAULT_POSTAL_CODE,
                    DEFAULT_LATITUDE,
                    DEFAULT_LONGITUDE,
                    DEFAULT_DMACODE
            )
    );
//    System.out.println("*****" + map.keySet());
//    super.refreshGeoData();
  }
  */

  @Override
  public String getOutputPrefix(CacheKey value) {
    return value.getField();
  }
}
