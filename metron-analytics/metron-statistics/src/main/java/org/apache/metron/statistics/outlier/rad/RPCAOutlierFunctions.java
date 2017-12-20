/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.statistics.outlier.rad;

import com.google.common.base.Joiner;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class RPCAOutlierFunctions {
  public enum Config {
     L_PENALTY("lpenalty", (o, outlier) -> outlier.withLPenalty(get("lpenalty", o, Double.class)))
    ,S_PENALTY("spenalty", (o, outlier) -> outlier.withSPenalty(get("spenalty", o, Double.class)))
    ,MIN_NONZERO("minNonZero", (o, outlier) -> outlier.withMinRecords(get("minNonZero", o, Integer.class)))
    ,FORCE_DIFF("forceDiff", (o, outlier) -> outlier.withForceDiff(get("forceDiff", o, Boolean.class)))
    ,TRIM("trim", (o, outlier) -> outlier.withTrim(get("trim", o, Boolean.class)))
    ;
    String key;
    BiFunction<Object, RPCAOutlier, RPCAOutlier> configure;
    Config(String key, BiFunction<Object, RPCAOutlier, RPCAOutlier> configure) {
      this.key = key;
      this.configure = configure;
    }

    private static <T> T get(String key, Object o, Class<T> clazz) {
      T v = ConversionUtils.convert(o, clazz);
      if(v == null) {
        throw new IllegalStateException(key + "(" + o + ") is expected to be of type " + clazz.getName() + " but was not");
      }
      return v;
    }

    public static RPCAOutlier configure(RPCAOutlier RPCAOutlier, Map<String, Object> config) {
      for(Config c : values()) {
        Object o = config.get(c.key);
        if(o != null) {
          RPCAOutlier = c.configure.apply(o, RPCAOutlier);
        }
      }
      return RPCAOutlier;
    }

    public static Iterable<String> getKeys() {
      List<String> ret = new ArrayList<>();
      for(Config c : values()) {
        ret.add(c.key);
      }
      return ret;
    }
  }
  @Stellar(namespace="OUTLIER"
          ,name="RPCA_SCORE"
          ,params = {
             "ts - The time series data to consider (an iterable of doubles).  Please ensure that it is largely time ordered."
          , "value - The value to score."
          , "config? - The config for the outlier analyzer in the form of a Map.  All of these have sensible defaults."
                     + "  Possible configs keys are "
                     + "\"lpenalty\", \"spenalty\" (see Zhou for more detail, defaults are sensible)"
                     + ", \"minNonZero\" (minimum number of non-zero elements, default is 0.)"
                     + ", \"forceDiff\" (force data to be stationary by differencing it.  See [here](https://people.duke.edu/~rnau/411diff.htm))."
                     + ", \"trim\" Trim data of leading and trailing 0s."
           }
          ,description= "This is an outlier detector based on Netflix's Surus' implementation of the Robust PCA-based Outlier Detector."
                      + "See [here](https://medium.com/netflix-techblog/rad-outlier-detection-on-big-data-d6b0494371cc)"
                      + " and [here](https://metamarkets.com/2012/algorithmic-trendspotting-the-meaning-of-interesting/) for a high level"
                      + " treatment of this approach.  A more formal treatment can be found at [Candes, Li, et al](http://statweb.stanford.edu/~candes/papers/RobustPCA.pdf)"
                      + " and [Zhou](http://arxiv.org/abs/1001.2363). Note: This is a computationally intense outlier detector, so "
                      + " it should be run if a less computationally intense detector has indicated a potential outlier (e.g. statistical baselining)."
                      + " Further note that the data input is presumed to be dense."
          ,returns="The residual error for the value.  Generally if > 0, then an there's an indication that it's an outlier."
  )
  public static class Score implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 2) {
        throw new IllegalStateException("Expected at minimum data series and value to score.");
      }
      Object dataObj = args.get(0);
      if(dataObj == null) {
        return Double.NaN;
      }
      Iterable<? extends Object> data = null;
      if(dataObj instanceof Iterable) {
        data = (Iterable<? extends Object>) dataObj;
      }
      else {
        throw new IllegalArgumentException("Data is expected to be an iterable of values convertible to double, but found " + dataObj.getClass().getName());
      }
      if(data == null) {
        return Double.NaN;
      }
      Object valueObj = args.get(1);
      if(valueObj == null) {
        return Double.NaN;
      }
      RPCAOutlier outlier = new RPCAOutlier();
      if(args.size() > 2) {
        Object configObj = args.get(2);
        if(!(configObj instanceof Map)) {
          throw new IllegalArgumentException("Config is expected to be a map with values "
                  + Joiner.on(",").join(Config.getKeys())
          );
        }
        Map<String, Object> config = (Map<String, Object>) configObj;
        if(config != null) {
          outlier = Config.configure(outlier, config);
        }
      }
      return outlier.outlierScore(data, valueObj);
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
