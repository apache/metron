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
package org.apache.metron.enrichment.adapters.geo.hash;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import com.google.common.collect.Iterables;
import org.apache.metron.enrichment.adapters.geo.GeoLiteDatabase;

import java.util.Map;
import java.util.Optional;

public enum GeoHashUtil {
  INSTANCE;

  public Optional<String> computeHash(Double latitude, Double longitude, int precision) {
    if(latitude == null || longitude == null) {
      return Optional.empty();
    }
    return computeHash(new WGS84Point(latitude, longitude), precision);
  }

  public Optional<String> computeHash(WGS84Point point, int precision) {
    GeoHash hash = GeoHash.withCharacterPrecision(point.getLatitude(), point.getLongitude(), precision);
    return Optional.of(hash.toBase32());
  }

  public Optional<String> computeHash(Map<String, String> geoLoc, int precision) {
    Optional<WGS84Point> point = GeoLiteDatabase.INSTANCE.toPoint(geoLoc);
    if(point.isPresent()) {
      return computeHash(point.get(), precision);
    }
    else {
      return Optional.empty();
    }
  }

  public Optional<WGS84Point> toPoint(String hash) {
    if(hash == null) {
      return Optional.empty();
    }
    GeoHash h = GeoHash.fromGeohashString(hash);
    return Optional.ofNullable(h == null?null:h.getPoint());
  }

  public double distance(WGS84Point point1, WGS84Point point2, DistanceStrategy strategy) {
    return strategy.distance(point1, point2);
  }

  public double maxDistanceHashes(Iterable<String> hashes, DistanceStrategy strategy) {
    Iterable<WGS84Point> points = Iterables.transform(hashes, s -> toPoint(s).orElse(null));
    return maxDistancePoints(Iterables.filter(points, p -> p != null), strategy);
  }

  public double maxDistancePoints(Iterable<WGS84Point> points, DistanceStrategy strategy) {
    //Note: because distance is commutative, we only need search the upper triangle
    int i = 0;
    double max = Double.NaN;
    for(WGS84Point pt1 : points) {
      int j = 0;
      for(WGS84Point pt2 : points) {
        if(j <= i) {
          double d = strategy.distance(pt1, pt2);
          if(Double.isNaN(max)|| d > max) {
            max = d;
          }
          j++;
        }
        else {
          break;
        }
      }
      i++;
    }
    return max;
  }
}
