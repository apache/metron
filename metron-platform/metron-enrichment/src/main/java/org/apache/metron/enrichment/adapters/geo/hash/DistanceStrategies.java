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
