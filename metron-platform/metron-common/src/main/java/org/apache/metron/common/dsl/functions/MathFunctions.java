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
 * Implements basic math functions.
 */
public class MathFunctions {

  public static class AddFunction implements Function<List<Object>, Object> {
    @Override
    public Object apply(List<Object> args) {

      if(args.get(0) instanceof Double) {
        Double left = ConversionUtils.convert(args.get(0), Double.class);
        Double right = ConversionUtils.convert(args.get(1), Double.class);
        return left + right;

      } else if(args.get(0) instanceof Float) {
        Float left = ConversionUtils.convert(args.get(0), Float.class);
        Float right = ConversionUtils.convert(args.get(1), Float.class);
        return left + right;

      } else if(args.get(0) instanceof Long) {
        Long left = ConversionUtils.convert(args.get(0), Long.class);
        Long right = ConversionUtils.convert(args.get(1), Long.class);
        return left + right;

      } else if(args.get(0) instanceof Integer) {
        Integer left = ConversionUtils.convert(args.get(0), Integer.class);
        Integer right = ConversionUtils.convert(args.get(1), Integer.class);
        return left + right;

      } else if(args.get(0) instanceof Short) {
        Short left = ConversionUtils.convert(args.get(0), Short.class);
        Short right = ConversionUtils.convert(args.get(1), Short.class);
        return left + right;

      } else {
        throw new RuntimeException(String.format(
                "Cannot apply AddFunction to '%s'",
                args.get(0).getClass().getSimpleName()));
      }
    }
  }

  public static class SubtractFunction implements Function<List<Object>, Object> {
    @Override
    public Object apply(List<Object> args) {

      if(args.get(0) instanceof Double) {
        Double left = ConversionUtils.convert(args.get(0), Double.class);
        Double right = ConversionUtils.convert(args.get(1), Double.class);
        return left - right;

      } else if(args.get(0) instanceof Float) {
        Float left = ConversionUtils.convert(args.get(0), Float.class);
        Float right = ConversionUtils.convert(args.get(1), Float.class);
        return left - right;

      } else if(args.get(0) instanceof Long) {
        Long left = ConversionUtils.convert(args.get(0), Long.class);
        Long right = ConversionUtils.convert(args.get(1), Long.class);
        return left - right;

      } else if(args.get(0) instanceof Integer) {
        Integer left = ConversionUtils.convert(args.get(0), Integer.class);
        Integer right = ConversionUtils.convert(args.get(1), Integer.class);
        return left - right;

      } else if(args.get(0) instanceof Short) {
        Short left = ConversionUtils.convert(args.get(0), Short.class);
        Short right = ConversionUtils.convert(args.get(1), Short.class);
        return left - right;

      } else {
        throw new RuntimeException(String.format(
                "Cannot apply SubtractFunction to '%s'",
                args.get(0).getClass().getSimpleName()));
      }
    }
  }

  public static class MultiplyFunction implements Function<List<Object>, Object> {
    @Override
    public Object apply(List<Object> args) {

      if(args.get(0) instanceof Double) {
        Double left = ConversionUtils.convert(args.get(0), Double.class);
        Double right = ConversionUtils.convert(args.get(1), Double.class);
        return left * right;

      } else if(args.get(0) instanceof Float) {
        Float left = ConversionUtils.convert(args.get(0), Float.class);
        Float right = ConversionUtils.convert(args.get(1), Float.class);
        return left * right;

      } else if(args.get(0) instanceof Long) {
        Long left = ConversionUtils.convert(args.get(0), Long.class);
        Long right = ConversionUtils.convert(args.get(1), Long.class);
        return left * right;

      } else if(args.get(0) instanceof Integer) {
        Integer left = ConversionUtils.convert(args.get(0), Integer.class);
        Integer right = ConversionUtils.convert(args.get(1), Integer.class);
        return left * right;

      } else if(args.get(0) instanceof Short) {
        Short left = ConversionUtils.convert(args.get(0), Short.class);
        Short right = ConversionUtils.convert(args.get(1), Short.class);
        return left * right;

      } else {
        throw new RuntimeException(String.format(
                "Cannot apply MultiplyFunction to '%s'",
                args.get(0).getClass().getSimpleName()));
      }
    }
  }

  public static class DivideFunction implements Function<List<Object>, Object> {
    @Override
    public Object apply(List<Object> args) {

      if(args.get(0) instanceof Double) {
        Double left = ConversionUtils.convert(args.get(0), Double.class);
        Double right = ConversionUtils.convert(args.get(1), Double.class);
        return left / right;

      } else if(args.get(0) instanceof Float) {
        Float left = ConversionUtils.convert(args.get(0), Float.class);
        Float right = ConversionUtils.convert(args.get(1), Float.class);
        return left / right;

      } else if(args.get(0) instanceof Long) {
        Long left = ConversionUtils.convert(args.get(0), Long.class);
        Long right = ConversionUtils.convert(args.get(1), Long.class);
        return left / right;

      } else if(args.get(0) instanceof Integer) {
        Integer left = ConversionUtils.convert(args.get(0), Integer.class);
        Integer right = ConversionUtils.convert(args.get(1), Integer.class);
        return left / right;

      } else if(args.get(0) instanceof Short) {
        Short left = ConversionUtils.convert(args.get(0), Short.class);
        Short right = ConversionUtils.convert(args.get(1), Short.class);
        return left / right;

      } else {
        throw new RuntimeException(String.format(
                "Cannot apply DivideFunction to '%s'",
                args.get(0).getClass().getSimpleName()));
      }
    }
  }
}
