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

import java.util.AbstractMap;
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

  public WGS84Point centroidOfHashes(Iterable<String> hashes) {
    Iterable<WGS84Point> points = Iterables.transform(hashes, h -> toPoint(h).orElse(null));
    return centroidOfPoints(points);
  }

  public WGS84Point centroidOfPoints(Iterable<WGS84Point> points) {
    Iterable<WGS84Point> nonNullPoints = Iterables.filter(points, p -> p != null);
    return centroid(Iterables.transform(nonNullPoints
                                       , p -> new AbstractMap.SimpleImmutableEntry<>(p, 1)
                                       )
                   );
  }

  public WGS84Point centroidOfWeightedPoints(Map<String, Number> points) {

    Iterable<Map.Entry<WGS84Point, Number>> weightedPoints = Iterables.transform(points.entrySet()
            , kv -> {
              WGS84Point pt = toPoint(kv.getKey()).orElse(null);
              return new AbstractMap.SimpleImmutableEntry<>(pt, kv.getValue());
            });
    return centroid(Iterables.filter(weightedPoints, kv -> kv.getKey() != null));
  }

  private WGS84Point centroid(Iterable<Map.Entry<WGS84Point, Number>> points) {
    double x = 0d
         , y = 0d
         , z = 0d
         , totalWeight = 0d
         ;
    int n = 0;
    for(Map.Entry<WGS84Point, Number> weightedPoint : points) {
      WGS84Point pt = weightedPoint.getKey();
      if(pt == null) {
        continue;
      }
      double latRad = Math.toRadians(pt.getLatitude());
      double longRad = Math.toRadians(pt.getLongitude());
      double cosLat = Math.cos(latRad);
      //convert from lat/long coordinates to cartesian coordinates
      double ptX = cosLat * Math.cos(longRad);
      double ptY = cosLat * Math.sin(longRad);
      double ptZ = Math.sin(latRad);
      double weight = weightedPoint.getValue().doubleValue();
      x += ptX*weight;
      y += ptY*weight;
      z += ptZ*weight;
      n++;
      totalWeight += weight;
    }
    if(n == 0) {
      return null;
    }
    //average the 3d cartesian vector representation
    x /= totalWeight;
    y /= totalWeight;
    z /= totalWeight;
    //convert the vector representation back
    double longitude = Math.atan2(y, x);
    double hypotenuse = Math.sqrt(x*x + y*y);
    double latitude = Math.atan2(z, hypotenuse);
    return new WGS84Point(Math.toDegrees(latitude), Math.toDegrees(longitude));
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
