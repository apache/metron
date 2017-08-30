package org.apache.metron.enrichment.stellar;

import ch.hsr.geohash.WGS84Point;
import org.apache.metron.enrichment.adapters.geo.hash.DistanceStrategies;
import org.apache.metron.enrichment.adapters.geo.hash.DistanceStrategy;
import org.apache.metron.enrichment.adapters.geo.hash.GeoHashUtil;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeoHashFunctions {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Stellar(name="FROM_LATLONG"
          ,namespace="GEOHASH"
          ,description="Compute geohash given a lat/long"
          ,params = {
                      "latitude - The latitude",
                      "longitude - The longitude",
                      "character_precision? - The number of characters to use in the hash. Default is 12"
                    }
          ,returns = "A geo hash of the lat/long"
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
          ,description="Compute geohash given a geo enrichment location"
          ,params = {
                      "map - the latitude and logitude in a map (the output of the Geo Enrichment)",
                      "character_precision? - The number of characters to use in the hash. Default is 12"
                    }
          ,returns = "A geo hash of the lat/long"
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
          ,description="Compute the distance between geo hashes"
          ,params = {
                      "hash1 - The first point as a geo hash",
                      "hash2 - The second point as a geo hash",
                      "strategy? - The distance to use.  One of HAVERSINE, LAW_OF_COSINES, or VICENTY.  Haversine is default."
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
          ,description="Compute the maximum distance among a list of hashes"
          ,params = {
                      "hashes - A set of hashes",
                      "strategy? - The distance strategy to use.  One of HAVERSINE, LAW_OF_COSINES, or VICENTY.  Haversine is default."
                    }
          ,returns = "The maximum distance in kilometers"
  )
  public static class MaxDist implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 1) {
        return null;
      }
      List<String> hashes = (List<String>)args.get(0);
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
}
