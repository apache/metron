package org.apache.metron.enrichment.stellar;

import ch.hsr.geohash.WGS84Point;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class GeoHashFunctionsTest {
  public static WGS84Point empireStatePoint = new WGS84Point(40.748570, -73.985752);
  public static WGS84Point mosconeCenterPoint = new WGS84Point(37.782891, -122.404166);
  public static WGS84Point jutlandPoint = new WGS84Point(57.64911, 10.40740);
  String empireStateHash = (String)StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat, long)"
                             , ImmutableMap.of("lat", empireStatePoint.getLatitude()
                                              ,"long",empireStatePoint.getLongitude()
                                              )
    );
  String mosconeCenterHash = (String)StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat, long)"
                             , ImmutableMap.of("lat", mosconeCenterPoint.getLatitude()
                                              ,"long",mosconeCenterPoint.getLongitude()
                                              )
    );
  String jutlandHash = (String)StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat, long)"
                             , ImmutableMap.of("lat", jutlandPoint.getLatitude()
                                              ,"long",jutlandPoint.getLongitude()
                                              )
  );

  @Test
  public void testHash_fromlatlong() throws Exception {
    Assert.assertEquals("u4pruydqmv", StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat, long, 10)"
                             , ImmutableMap.of("lat", jutlandPoint.getLatitude()
                                              ,"long",jutlandPoint.getLongitude()
                                              )
                             )
    );

    Assert.assertEquals("u4pruydqmvpb", StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat, long)"
                             , ImmutableMap.of("lat", jutlandPoint.getLatitude()
                                              ,"long",jutlandPoint.getLongitude()
                                              )
                             )
    );
    Assert.assertEquals("u4pruydqmv".substring(0, 6), StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat, long, 6)"
                             , ImmutableMap.of("lat", jutlandPoint.getLatitude()
                                              ,"long",jutlandPoint.getLongitude()
                                              )
                             )
    );
    Assert.assertNull(StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat)"
                             , ImmutableMap.of("lat", jutlandPoint.getLatitude()
                                              )
                             )
    );
    Assert.assertNull(StellarProcessorUtils.run("GEOHASH_FROM_LATLONG(lat, long, 10)"
                             , ImmutableMap.of("lat", "blah"
                                              ,"long",jutlandPoint.getLongitude()
                                              )
                             )
    );
  }

  @Test
  public void testHash_fromLocation() throws Exception {
    Map<String, String> loc = ImmutableMap.of( "latitude", "" + jutlandPoint.getLatitude()
                                             , "longitude","" + jutlandPoint.getLongitude()
                                                                     );
    Assert.assertEquals("u4pruydqmv", StellarProcessorUtils.run("GEOHASH_FROM_LOC(loc, 10)"
                             , ImmutableMap.of("loc", loc
                                              )
                             )
    );

    Assert.assertEquals("u4pruydqmv".substring(0, 6), StellarProcessorUtils.run("GEOHASH_FROM_LOC(loc, 6)"
                             , ImmutableMap.of("loc", loc
                                              )
                             )
    );

    Assert.assertEquals("u4pruydqmvpb", StellarProcessorUtils.run("GEOHASH_FROM_LOC(loc)"
                             , ImmutableMap.of("loc", loc
                                              )
                             )
    );
    Assert.assertNull(StellarProcessorUtils.run("GEOHASH_FROM_LOC(loc)"
                                               , ImmutableMap.of("loc", ImmutableMap.of( "latitude", "57.64911" ))
                             )
    );
    Assert.assertNull(StellarProcessorUtils.run("GEOHASH_FROM_LOC(loc, 10)"
                                                , ImmutableMap.of("loc", ImmutableMap.of( "latitude", "blah"
                                                                                        , "longitude","10.40740"
                                                                     )
                                              )

                             )
    );
  }

  @Test
  public void testDistanceHaversine() throws Exception {
    testDistance(Optional.empty());
    testDistance(Optional.of("HAVERSINE"));
  }

  @Test
  public void testDistanceLawOfCosines() throws Exception {
    testDistance(Optional.of("LAW_OF_COSINES"));
  }

  @Test
  public void testDistanceLawOfVicenty() throws Exception {
    testDistance(Optional.of("VICENTY"));
  }

  @Test
  public void testMaxDistance_happyPath() throws Exception {
    Double maxDistance = (double) StellarProcessorUtils.run("GEOHASH_MAX_DIST([empireState, mosconeCenter, jutland])"
            , ImmutableMap.of("empireState", empireStateHash
                    , "mosconeCenter", mosconeCenterHash
                    , "jutland", jutlandHash
            )
    );
    double expectedDistance = 8528;
    Assert.assertEquals(expectedDistance, maxDistance, 1d);
  }

  @Test
  public void testMaxDistance_differentOrder() throws Exception {
    Double maxDistance = (double) StellarProcessorUtils.run("GEOHASH_MAX_DIST([jutland, mosconeCenter, empireState])"
            , ImmutableMap.of("empireState", empireStateHash
                    , "mosconeCenter", mosconeCenterHash
                    , "jutland", jutlandHash
            )
    );
    double expectedDistance = 8528;
    Assert.assertEquals(expectedDistance, maxDistance, 1d);
  }

  @Test
  public void testMaxDistance_withNulls() throws Exception {
    Double maxDistance = (double) StellarProcessorUtils.run("GEOHASH_MAX_DIST([jutland, mosconeCenter, empireState, null])"
            , ImmutableMap.of("empireState", empireStateHash
                    , "mosconeCenter", mosconeCenterHash
                    , "jutland", jutlandHash
            )
    );
    double expectedDistance = 8528;
    Assert.assertEquals(expectedDistance, maxDistance, 1d);
  }
  @Test
  public void testMaxDistance_allSame() throws Exception {
    Double maxDistance = (double) StellarProcessorUtils.run("GEOHASH_MAX_DIST([jutland, jutland, jutland])"
            , ImmutableMap.of( "jutland", jutlandHash )
    );
    Assert.assertEquals(0, maxDistance, 1e-6d);
  }

  @Test
  public void testMaxDistance_emptyList() throws Exception {
    Double maxDistance = (double) StellarProcessorUtils.run("GEOHASH_MAX_DIST([])" , new HashMap<>() );
    Assert.assertTrue(Double.isNaN(maxDistance));
  }

  @Test
  public void testMaxDistance_nullList() throws Exception {
    Double maxDistance = (Double) StellarProcessorUtils.run("GEOHASH_MAX_DIST(null)" , new HashMap<>() );
    Assert.assertNull(maxDistance);
  }

  @Test
  public void testMaxDistance_invalidList() throws Exception {
    Double maxDistance = (Double) StellarProcessorUtils.run("GEOHASH_MAX_DIST()" , new HashMap<>() );
    Assert.assertNull(maxDistance);
  }

  public void testDistance(Optional<String> method) throws Exception {
    double expectedDistance = 4128; //in kilometers
    Map<String, Object> vars = ImmutableMap.of("empireState", empireStateHash, "mosconeCenter", mosconeCenterHash);
    //ensure that d(x, y) == d(y, x) and that both are the same as the expected (up to 1 km accuracy)
    {
      String stellarStatement = getDistStellarStatement(ImmutableList.of("mosconeCenter", "empireState"), method);
      Assert.assertEquals(expectedDistance, (double) StellarProcessorUtils.run(stellarStatement , vars ), 1D );
    }
    {
      String stellarStatement = getDistStellarStatement(ImmutableList.of("empireState", "mosconeCenter"), method);
      Assert.assertEquals(expectedDistance, (double) StellarProcessorUtils.run(stellarStatement , vars ), 1D );
    }
  }

  private static String getDistStellarStatement(List<String> hashVariables, Optional<String> method) {
    if(method.isPresent()) {
      List<String> vars = new ArrayList<>();
      vars.addAll(hashVariables);
      vars.add("\'" + method.get() + "\'");
      return "GEOHASH_DIST(" + Joiner.on(",").skipNulls().join(vars) + ")";
    }
    else {
      return "GEOHASH_DIST(" + Joiner.on(",").skipNulls().join(hashVariables) + ")";
    }
  }
}
