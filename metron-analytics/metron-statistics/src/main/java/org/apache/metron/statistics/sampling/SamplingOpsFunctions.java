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

import com.codahale.metrics.Reservoir;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.List;
import java.util.Optional;

public class SamplingOpsFunctions {

  @Stellar(namespace="SAMPLE"
          ,name="GET"
          ,description="Return the sample."
          ,params = {
            "sampler - Sampler to use."
          }
          ,returns="The resulting sample."
  )
  public static class Get implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() == 0) {
        return null;
      }

      Sampler s = null;
      Object sObj = args.get(0);
      if(sObj == null) {
        return null;
      }
      else if(sObj instanceof Sampler) {
        s = (Sampler)sObj;
      }
      else {
        throw new IllegalStateException("Expected a sampler, but found " + sObj);
      }
      return s.get();
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(namespace="SAMPLE"
          ,name="ADD"
          ,description="Add a value or collection of values to a sampler."
          ,params = {
            "sampler - Sampler to use.  If null, then a default Uniform sampler is created."
            ,"o - The value to add.  If o is an Iterable, then each item is added."
          }
          ,returns="The sampler."
  )
  public static class Add implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() == 0) {
        return null;
      }
      if(args.size() < 2) {
        throw new IllegalStateException("Expected sampler and value to add");
      }
      Sampler s = null;
      Object sObj = args.get(0);
      if(sObj == null) {
        s = new UniformSampler();
      }
      else if(sObj instanceof Sampler) {
        s = (Sampler)sObj;
      }
      else {
        throw new IllegalStateException("Expected a sampler, but found " + sObj);
      }
      Object valsObj = args.get(1);
      if(valsObj == null) {
        return s;
      }
      else if(valsObj instanceof Iterable) {
        Iterable<Object> vals = (Iterable<Object>)valsObj;
        s.addAll(vals);
      }
      else {
        s.add(valsObj);
      }
      return s;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(namespace="SAMPLE"
          ,name="MERGE"
          ,description="Merge and resample a collection of samples."
          ,params = {
            "samplers - A list of samplers to merge."
          , "baseSampler? - A base sampler to merge into.  If unspecified the first of the list of samplers will be cloned."
          }
          ,returns = "A sampler which represents the resampled merger of the samplers."
  )
  public static class Merge implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() == 0) {
        return null;
      }
      Object reservoirsObj = args.get(0);
      if(reservoirsObj == null) {
        return null;
      }
      if(!(reservoirsObj instanceof Iterable)){
        throw new IllegalStateException("Expected a collection of Samplers");
      }
      Iterable<Sampler> reservoirs = (Iterable<Sampler>)reservoirsObj;

      Sampler baseSampler = null;
      if(args.size() > 1) {
        Object baseSamplerObj = args.get(1);
        if (baseSamplerObj != null) {
          if (!(baseSamplerObj instanceof Sampler)) {
            throw new IllegalStateException("Expected baseSampler to be a Sampler");
          } else {
            baseSampler = (Sampler) baseSamplerObj;
          }
        }
      }
      return SamplerUtil.INSTANCE.merge(reservoirs, Optional.ofNullable(baseSampler));
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
