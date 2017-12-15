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
package org.apache.metron.statistics.outlier.rpca;

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
    ,S_PENALTY("rpenalty", (o, outlier) -> outlier.withSPenalty(get("spenalty", o, Double.class)))
    ,MIN_NONZERO("minNonZero", (o, outlier) -> outlier.withMinRecords(get("minNonZero", o, Integer.class)))
    ,FORCE_DIFF("forceDiff", (o, outlier) -> outlier.withForceDiff(get("forceDiff", o, Boolean.class)))
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

    public static RPCAOutlier configure(RPCAOutlier rpcaOutlier, Map<String, Object> config) {
      for(Config c : values()) {
        Object o = config.get(c.key);
        if(o != null) {
          rpcaOutlier = c.configure.apply(o, rpcaOutlier);
        }
      }
      return rpcaOutlier;
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
             "data - The data to consider"
          , "value - The value to compare"
          , "config? - The config"
           }
          ,description="RPCA"
          ,returns="The RPCA score."
  )
  public static class Score implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 2) {
        throw new IllegalStateException("Expected at minimum data series and value to score.");
      }
      Object dataObj = args.get(0);
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
