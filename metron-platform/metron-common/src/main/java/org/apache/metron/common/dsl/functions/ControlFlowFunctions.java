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

package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.utils.ConversionUtils;

import java.util.List;
import java.util.function.Function;

/**
 * Functions that provide functionality related to control flow.
 */
public class ControlFlowFunctions {

  public static class IfThenElseFunction implements Function<List<Object>, Object> {
    @Override
    public Object apply(List<Object> args) {

      Object ifExpression = args.get(0);
      Object thenExpression = args.get(1);
      Object elseExpression = args.get(2);

      Boolean ifCondition = ConversionUtils.convert(ifExpression, Boolean.class);
      if(ifCondition) {
        return thenExpression;
      } else {
        return elseExpression;
      }
    }
  }

  public static class EqualsFunction implements Function<List<Object>, Object> {
    @Override
    public Object apply(List<Object> args) {
      Object left = args.get(0);
      Object right = args.get(1);
      return left.equals(right);
    }
  }
}
