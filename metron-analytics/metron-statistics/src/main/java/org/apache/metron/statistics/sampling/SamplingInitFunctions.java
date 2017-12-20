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
package org.apache.metron.statistics.sampling;

import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SamplingInitFunctions {

  @Stellar(namespace="SAMPLE"
          ,name="INIT"
          ,description="Create a [reservoir sampler](https://en.wikipedia.org/wiki/Reservoir_sampling) of a specific size or, if unspecified, size " + Sampler.DEFAULT_SIZE + ".  Elements sampled by the reservoir sampler will be included in the final sample with equal probability."
          ,params = {
            "size? - The size of the reservoir sampler.  If unspecified, the size is " + Sampler.DEFAULT_SIZE
          }
          ,returns="The sampler object."
  )

  public static class UniformSamplerInit implements StellarFunction {
    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() == 0) {
        return new UniformSampler();
      }
      else {
        Optional<Integer> sizeArg = get(args, 0, "Size", Integer.class);
        if(sizeArg.isPresent() && sizeArg.get() <= 0) {
          throw new IllegalStateException("Size must be a positive integer");
        }
        else {
          return new UniformSampler(sizeArg.orElse(Sampler.DEFAULT_SIZE));
        }
      }
    }

    @Override
    public void initialize(Context context) {
    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }


  public static <T> Optional<T> get(List<Object> args, int offset, String argName, Class<T> expectedClazz) {
    Object obj = args.get(offset);
    T ret = ConversionUtils.convert(obj, expectedClazz);
    if(ret == null ) {
      if(obj != null) {
        throw new IllegalStateException(argName + "argument(" + obj
                                       + " is expected to be an " + expectedClazz.getName()
                                       + ", but was " + obj
                                       );
      }
    }
    return Optional.ofNullable(ret);
  }
}
