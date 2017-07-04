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
package org.apache.metron.stellar.dsl.functions;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.List;

public class MathFunctions {

  @Stellar(name="ABS"
          ,description="Returns the absolute value of a number."
          ,params = {
                "number - The number to take the absolute value of"
                    }
          , returns="The absolute value of the number passed in."
          )
  public static class Abs implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 1) {
        return Double.NaN;
      }
      Number n = (Number)args.get(0);
      if(n == null) {
        return Double.NaN;
      }
      return Math.abs(n.doubleValue());
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
