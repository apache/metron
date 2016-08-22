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

package org.apache.metron.common.dsl.functions;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.metron.common.dsl.BaseStellarFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class StringFunctions {
  public static class RegexpMatch extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        throw new IllegalStateException("REGEXP_MATCH expects two args: [string, pattern] where pattern is a regexp pattern");
      }
      String pattern = (String) list.get(1);
      String str = (String) list.get(0);
      if(str == null || pattern == null) {
        return false;
      }
      return str.matches(pattern);
    }
  }

  public static class EndsWith extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        throw new IllegalStateException("ENDS_WITH expects two args: [string, suffix] where suffix is the string fragment that the string should end with");
      }
      String prefix = (String) list.get(1);
      String str = (String) list.get(0);
      if(str == null || prefix == null) {
        return false;
      }
      return str.endsWith(prefix);
    }
  }
  public static class StartsWith extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        throw new IllegalStateException("STARTS_WITH expects two args: [string, prefix] where prefix is the string fragment that the string should start with");
      }
      String prefix = (String) list.get(1);
      String str = (String) list.get(0);
      if(str == null || prefix == null) {
        return false;
      }
      return str.startsWith(prefix);
    }
  }
  public static class ToLower extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString().toLowerCase();
    }
  }
  public static class ToUpper extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString().toUpperCase();
    }
  }
  public static class ToString extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString();
    }
  }
  public static class Trim extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString().trim();
    }
  }
  public static class JoinFunction extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      String delim = (String) args.get(1);
      return Joiner.on(delim).join(Iterables.filter(arg1, x -> x != null));
    }
  }
  public static class SplitFunction extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List ret = new ArrayList();
      Object o1 = args.get(0);
      if(o1 != null) {
        String arg1 = o1.toString();
        String delim = (String) args.get(1);
        Iterables.addAll(ret, Splitter.on(delim).split(arg1));
      }
      return ret;
    }
  }

  public static class GetLast extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      return Iterables.getLast(arg1, null);
    }
  }
  public static class GetFirst extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      return Iterables.getFirst(arg1, null);
    }
  }

  public static class Get extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      int offset = (Integer) args.get(1);
      if(offset < arg1.size()) {
        return Iterables.get(arg1, offset);
      }
      return null;
    }
  }
}
