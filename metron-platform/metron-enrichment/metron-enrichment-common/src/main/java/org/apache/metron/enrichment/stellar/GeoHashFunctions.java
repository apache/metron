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
package org.apache.metron.enrichment.stellar;

import ch.hsr.geohash.WGS84Point;
import org.apache.metron.enrichment.adapters.maxmind.geo.GeoLiteCityDatabase;
import org.apache.metron.enrichment.adapters.maxmind.geo.hash.DistanceStrategies;
import org.apache.metron.enrichment.adapters.maxmind.geo.hash.DistanceStrategy;
import org.apache.metron.enrichment.adapters.maxmind.geo.hash.GeoHashUtil;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeoHashFunctions {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Stellar(name="TO_LATLONG"
          ,namespace="GEOHASH"
          ,description="Compute the lat/long of a given [geohash](https://en.wikipedia.org/wiki/Geohash)"
          ,params = {
                      "hash - The [geohash](https://en.wikipedia.org/wiki/Geohash)"
                    }
          ,returns = "A map containing the latitude and longitude of the hash (keys \"latitude\" and \"longitude\")"
  )
  public static class ToLatLong implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 1) {
        return null;
      }
      String hash = (String)args.get(0);
      if(hash == null) {
        return null;
      }

      Optional<WGS84Point> point = GeoHashUtil.INSTANCE.toPoint(hash);
      if(point.isPresent()) {
        Map<String, Object> ret = new HashMap<>();
        ret.put(GeoLiteCityDatabase.GeoProps.LONGITUDE.getSimpleName(), point.get().getLongitude());
        ret.put(GeoLiteCityDatabase.GeoProps.LATITUDE.getSimpleName(), point.get().getLatitude());
        return ret;
      }
      return null;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(name="FROM_LATLONG"
          ,namespace="GEOHASH"
          ,description="Compute [geohash](https://en.wikipedia.org/wiki/Geohash) given a lat/long"
          ,params = {
                      "latitude - The latitude",
                      "longitude - The longitude",
                      "character_precision? - The number of characters to use in the hash. Default is 12"
                    }
          ,returns = "A [geohash](https://en.wikipedia.org/wiki/Geohash) of the lat/long"
  )
  public static class FromLatLong implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 2) {
        return null;
      }
      Object latObj = args.get(0);
      Object longObj = args.get(1);
      if(latObj == null || longObj == null) {
        return null;
      }
      Double latitude = ConversionUtils.convert(latObj, Double.class);
      Double longitude = ConversionUtils.convert(longObj, Double.class);
      int charPrecision = 12;
      if(args.size() > 2) {
        charPrecision = ConversionUtils.convert(args.get(2), Integer.class);
      }
      Optional<String> ret = GeoHashUtil.INSTANCE.computeHash(latitude, longitude, charPrecision);
      return ret.orElse(null);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(name="FROM_LOC"
          ,namespace="GEOHASH"
          ,description="Compute [geohash](https://en.wikipedia.org/wiki/Geohash) given a geo enrichment location"
          ,params = {
                      "map - the latitude and logitude in a map (the output of GEO_GET)",
                      "character_precision? - The number of characters to use in the hash. Default is 12"
                    }
          ,returns = "A [geohash](https://en.wikipedia.org/wiki/Geohash) of the location"
  )
  public static class FromLoc implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 1) {
        return null;
      }
      Map<String, String> map = (Map<String, String>) args.get(0);
      if(map == null) {
        return null;
      }
      int charPrecision = 12;
      if(args.size() > 1) {
        charPrecision = ConversionUtils.convert(args.get(1), Integer.class);
      }
      Optional<String> ret = GeoHashUtil.INSTANCE.computeHash(map, charPrecision);
      return ret.orElse(null);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }


  @Stellar(name="DIST"
          ,namespace="GEOHASH"
          ,description="Compute the distance between [geohashes](https://en.wikipedia.org/wiki/Geohash)"
          ,params = {
                      "hash1 - The first location as a geohash",
                      "hash2 - The second location as a geohash",
                      "strategy? - The great circle distance strategy to use. One of [HAVERSINE](https://en.wikipedia.org/wiki/Haversine_formula), [LAW_OF_COSINES](https://en.wikipedia.org/wiki/Law_of_cosines#Using_the_distance_formula), or [VICENTY](https://en.wikipedia.org/wiki/Vincenty%27s_formulae).  Haversine is default."
                    }
          ,returns = "The distance in kilometers between the hashes"
  )
  public static class Dist implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 2) {
        return null;
      }
      String hash1 = (String)args.get(0);
      if(hash1 == null) {
        return null;
      }
      Optional<WGS84Point> pt1 = GeoHashUtil.INSTANCE.toPoint(hash1);
      String hash2 = (String)args.get(1);
      if(hash2 == null) {
        return null;
      }
      Optional<WGS84Point> pt2 = GeoHashUtil.INSTANCE.toPoint(hash2);
      DistanceStrategy strat = DistanceStrategies.HAVERSINE;
      if(args.size() > 2) {
        strat = DistanceStrategies.valueOf((String) args.get(2));
      }
      if(pt1.isPresent() && pt2.isPresent()) {
        return GeoHashUtil.INSTANCE.distance(pt1.get(), pt2.get(), strat);
      }
      return Double.NaN;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(name="MAX_DIST"
          ,namespace="GEOHASH"
          ,description="Compute the maximum distance among a list of [geohashes](https://en.wikipedia.org/wiki/Geohash)"
          ,params = {
                      "hashes - A collection of [geohashes](https://en.wikipedia.org/wiki/Geohash)",
                      "strategy? - The great circle distance strategy to use. One of [HAVERSINE](https://en.wikipedia.org/wiki/Haversine_formula), [LAW_OF_COSINES](https://en.wikipedia.org/wiki/Law_of_cosines#Using_the_distance_formula), or [VICENTY](https://en.wikipedia.org/wiki/Vincenty%27s_formulae).  Haversine is default."
                    }
          ,returns = "The maximum distance in kilometers between any two locations"
  )
  public static class MaxDist implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 1) {
        return null;
      }
      Iterable<String> hashes = (Iterable<String>)args.get(0);
      if(hashes == null) {
        return null;
      }
      DistanceStrategy strat = DistanceStrategies.HAVERSINE;
      if(args.size() > 1) {
        strat = DistanceStrategies.valueOf((String) args.get(1));
      }
      return GeoHashUtil.INSTANCE.maxDistanceHashes(hashes, strat);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(name="CENTROID"
          ,namespace="GEOHASH"
          ,description="Compute the centroid (geographic midpoint or center of gravity) of a set of [geohashes](https://en.wikipedia.org/wiki/Geohash)"
          ,params = {
                      "hashes - A collection of [geohashes](https://en.wikipedia.org/wiki/Geohash) or a map associating geohashes to numeric weights"
                     ,"character_precision? - The number of characters to use in the hash. Default is 12"
                    }
          ,returns = "The geohash of the centroid"
  )
  public static class Centroid implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 1) {
        return null;
      }
      Object o1 = args.get(0);
      if(o1 == null) {
        return null;
      }
      WGS84Point centroid = null;
      if(o1 instanceof Map) {
         centroid = GeoHashUtil.INSTANCE.centroidOfWeightedPoints((Map<String, Number>)o1);
      }
      else if(o1 instanceof Iterable) {
        centroid = GeoHashUtil.INSTANCE.centroidOfHashes((Iterable<String>)o1);
      }
      if(centroid == null) {
        return null;
      }
      Integer precision = 12;
      if(args.size() > 1) {
        precision = (Integer)args.get(1);
      }
      return GeoHashUtil.INSTANCE.computeHash(centroid, precision).orElse(null);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }
}
