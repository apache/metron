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

  /**
   * Find the equilibrium point of a weighted set of lat/long geo points.
   * @param points  The points and their weights (e.g. multiplicity)
   * @return
   */
  private WGS84Point centroid(Iterable<Map.Entry<WGS84Point, Number>> points) {
    double x = 0d
         , y = 0d
         , z = 0d
         , totalWeight = 0d
         ;
    int n = 0;
    /**
     * So, it's first important to realize that long/lat are not cartesian, so simple weighted averaging
     * is insufficient here as it denies the fact that we're not living on a flat square, but rather the surface of
     * an ellipsoid.  A crow, for instance, does not fly a straight line to an observer outside of Earth, but
     * rather flies across the arc tracing the surface of earth, or a "great-earth arc".  When computing the centroid
     * you want to find the centroid of the points with distance defined as the great-earth arc.
     *
     * The general strategy is to:
     * 1. Change coordinate systems from degrees on a WGS84 projection (e.g. lat/long)
     *    to a 3 dimensional cartesian surface atop a sphere approximating the earth.
     * 2. Compute a weighted average of the cartesian coordinates
     * 3. Change coordinate systems of the resulting centroid in cartesian space back to lat/long
     *
     * This is generally detailed at http://www.geomidpoint.com/example.html
     */
    for(Map.Entry<WGS84Point, Number> weightedPoint : points) {
      WGS84Point pt = weightedPoint.getKey();
      if(pt == null) {
        continue;
      }
      double latRad = Math.toRadians(pt.getLatitude());
      double longRad = Math.toRadians(pt.getLongitude());
      double cosLat = Math.cos(latRad);
      /*
       Convert from lat/long coordinates to cartesian coordinates.  The cartesian coordinate system is a right-hand,
       rectangular, three-dimensional, earth-fixed coordinate system
       with an origin at (0, 0, 0). The Z-axis, is parrallel to the axis of rotation of the earth. The Z-coordinate
       is positive toward the North pole. The X-Y plane lies in the equatorial plane. The X-axis lies along the
       intersection of the plane containing the prime meridian and the equatorial plane. The X-coordinate is positive
       toward the intersection of the prime meridian and equator.

       Please see https://en.wikipedia.org/wiki/Geographic_coordinate_conversion#From_geodetic_to_ECEF_coordinates
       for more information about this coordinate transformation.
       */
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
    //average the vector representation in cartesian space, forming the center of gravity in cartesian space
    x /= totalWeight;
    y /= totalWeight;
    z /= totalWeight;

    //convert the cartesian representation back to radians
    double longitude = Math.atan2(y, x);
    double hypotenuse = Math.sqrt(x*x + y*y);
    double latitude = Math.atan2(z, hypotenuse);

    //convert the radians back to degrees latitude and longitude.
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
