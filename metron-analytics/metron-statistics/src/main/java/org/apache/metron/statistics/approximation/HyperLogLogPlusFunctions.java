/**
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
package org.apache.metron.statistics.approximation;

import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.utils.ConversionUtils;

import java.util.ArrayList;
import java.util.List;

public class HyperLogLogPlusFunctions {

  @Stellar(namespace = "HLLP"
          , name = "ADD"
          , description = "Add value to the HyperLogLogPlus estimator set. See [HLLP README](HLLP.md)"
          , params = {
              "hyperLogLogPlus - the hllp estimator to add a value to"
            , "value* - value to add to the set. Takes a single item or a list."
            }
          , returns = "The HyperLogLogPlus set with a new value added"
  )
  public static class HLLPAdd extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() < 2) {
        throw new IllegalArgumentException("Must pass an hllp estimator set and at least one value to add to the set");
      } else {
        HyperLogLogPlus hllp = ConversionUtils.convert(args.get(0), HyperLogLogPlus.class);
        Object secondArg = args.get(1);
        if (secondArg instanceof List) {
          hllp.addAll((List) secondArg);
        } else {
          hllp.add(secondArg);
        }
        return hllp;
      }
    }
  }

  @Stellar(namespace = "HLLP"
          , name = "CARDINALITY"
          , description = "Returns HyperLogLogPlus-estimated cardinality for this set. See [HLLP README](HLLP.md)"
          , params = {"hyperLogLogPlus - the hllp set"}
          , returns = "Long value representing the cardinality for this set"
  )
  public static class HLLPCardinality extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() < 1) {
        throw new IllegalArgumentException("Must pass an hllp set to get the cardinality for");
      }
      return ((HyperLogLogPlus) args.get(0)).cardinality();
    }
  }

  @Stellar(namespace = "HLLP"
          , name = "INIT"
          , description = "Initializes the HyperLogLogPlus estimator set. p must be a value between 4 and sp and sp must be less than 32 and greater than 4. See [HLLP README](HLLP.md)"
          , params = {
              "p (required) - the precision value for the normal set"
            , "sp - the precision value for the sparse set. If sp is 0 or not specified, the sparse set will be disabled."
            }
          , returns = "A new HyperLogLogPlus set"
  )
  public static class HLLPInit extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() == 0) {
        throw new IllegalArgumentException("Normal set precision is required");
      } else if (args.size() == 1) {
        int p = ConversionUtils.convert(args.get(0), Integer.class);
        return new HyperLogLogPlus(p);
      } else {
        int p = ConversionUtils.convert(args.get(0), Integer.class);
        int sp = ConversionUtils.convert(args.get(1), Integer.class);
        return new HyperLogLogPlus(p, sp);
      }
    }
  }

  @Stellar(namespace = "HLLP"
          , name = "MERGE"
          , description = "Merge hllp sets together. The resulting estimator is initialized with p and sp precision values from the first provided hllp estimator set. See [HLLP README](HLLP.md)"
          , params = {"hllp* - List of hllp estimators to merge"}
          , returns = "A new merged HyperLogLogPlus estimator set"
  )
  public static class HLLPMerge extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() < 1) {
        throw new IllegalArgumentException("Must pass 1..n hllp sets to merge");
      } else {
        List<Object> estimators = new ArrayList();
        if (args.get(0) instanceof List) {
          estimators = (List) args.get(0);
        } else {
          estimators.add(args.get(0));
        }
        if (estimators.size() < 1) {
          throw new IllegalArgumentException("Must pass 1..n hllp sets to merge");
        }
        HyperLogLogPlus hllp = ConversionUtils.convert(estimators.get(0), HyperLogLogPlus.class);
        if (estimators.size() > 1) {
          hllp = hllp.merge(getEstimatorsFromIndex(estimators, 1));
        }
        return hllp;
      }
    }

    /**
     * Get sublist starting at index and convert types
     */
    private List<HyperLogLogPlus> getEstimatorsFromIndex(List<Object> args, int index) {
      return ConversionUtils.convertList(args.subList(index, args.size()), HyperLogLogPlus.class);
    }
  }

}
