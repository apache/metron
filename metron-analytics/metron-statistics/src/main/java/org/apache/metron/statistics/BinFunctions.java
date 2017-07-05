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
package org.apache.metron.statistics;

import org.apache.metron.stellar.dsl.*;

import java.util.List;
import java.util.function.Function;

import static org.apache.metron.stellar.common.utils.ConversionUtils.convert;

public class BinFunctions {

  /**
   * Calculates the statistical bin that a value falls in.
   */
  @Stellar(name = "BIN"
          , description = "Computes the bin that the value is in given a set of bounds."
          , params = {
           "value - The value to bin"
          , "bounds - A list of value bounds (excluding min and max) in sorted order."
                    }
          ,returns = "Which bin N the value falls in such that bound(N-1) < value <= bound(N). " +
          "No min and max bounds are provided, so values smaller than the 0'th bound go in the 0'th bin, " +
          "and values greater than the last bound go in the M'th bin."
  )
  public static class Bin extends BaseStellarFunction {

    public static int getBin(double value, int numBins, Function<Integer, Double> boundFunc) {
      double lastBound = Double.NEGATIVE_INFINITY;
      for(int bin = 0; bin < numBins;++bin) {
        double bound = boundFunc.apply(bin);
        if(lastBound > bound ) {
          throw new IllegalStateException("Your bins must be non-decreasing");
        }
        if(value <= bound) {
          return bin;
        }
        lastBound = bound;
      }
      return numBins;
    }

    @Override
    public Object apply(List<Object> args) {
      Double value = convert(args.get(0), Double.class);
      final List<Number> bins = args.size() > 1?convert(args.get(1), List.class):null;
      if ( value == null || bins == null || bins.size() == 0) {
        return -1;
      }
      return getBin(value, bins.size(), bin -> bins.get(bin).doubleValue());
    }
  }
}
