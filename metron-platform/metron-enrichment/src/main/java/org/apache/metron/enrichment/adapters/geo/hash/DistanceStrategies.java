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

import ch.hsr.geohash.WGS84Point;
import org.locationtech.spatial4j.distance.DistanceUtils;

public enum DistanceStrategies implements DistanceStrategy {
  HAVERSINE((p1, p2) -> DistanceUtils.EARTH_MEAN_RADIUS_KM*DistanceUtils.distHaversineRAD( Math.toRadians(p1.getLatitude()), Math.toRadians(p1.getLongitude())
                                                 , Math.toRadians(p2.getLatitude()), Math.toRadians(p2.getLongitude())
                                                 )
          ),
  LAW_OF_COSINES((p1, p2) -> DistanceUtils.EARTH_MEAN_RADIUS_KM*DistanceUtils.distLawOfCosinesRAD( Math.toRadians(p1.getLatitude()), Math.toRadians(p1.getLongitude())
                                                 , Math.toRadians(p2.getLatitude()), Math.toRadians(p2.getLongitude())
                                                 )
          ),
  VICENTY((p1, p2) -> DistanceUtils.EARTH_MEAN_RADIUS_KM*DistanceUtils.distVincentyRAD( Math.toRadians(p1.getLatitude()), Math.toRadians(p1.getLongitude())
                                                 , Math.toRadians(p2.getLatitude()), Math.toRadians(p2.getLongitude())
                                                 )
          )
  ;
  DistanceStrategy strat;
  DistanceStrategies(DistanceStrategy strat) {
    this.strat = strat;
  }

  @Override
  public double distance(WGS84Point point1, WGS84Point point2) {
    return strat.distance(point1, point2);
  }
}
