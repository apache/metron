package org.apache.metron.enrichment.adapters.geo.hash;

import ch.hsr.geohash.WGS84Point;

public interface DistanceStrategy {
  public double distance(WGS84Point point1, WGS84Point point2);
}
