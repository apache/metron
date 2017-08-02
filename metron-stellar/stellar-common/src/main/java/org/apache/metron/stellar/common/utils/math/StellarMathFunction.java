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
package org.apache.metron.stellar.common.utils.math;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.List;

public class StellarMathFunction implements StellarFunction {
  MathOperation _func;
  public StellarMathFunction(MathOperations _func) {
    this._func = _func.op;
  }

  public StellarMathFunction(MathOperation _func) {
    this._func = _func;
  }

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    if(args.size() < _func.getMinArgs()) {
      return Double.NaN;
    }
    Number[] nums = new Number[_func.getMaxArgs()];
    for(int i = 0; i < _func.getMaxArgs();++i) {
      nums[i] = (Number)args.get(i);
      if(nums[i] == null) {
        return Double.NaN;
      }
    }

    Object ret = _func.getOperation().apply(nums);
    return ret;
  }

  @Override
  public void initialize(Context context) {

  }

  @Override
  public boolean isInitialized() {
    return true;
  }
}
