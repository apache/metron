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
package org.apache.metron.management;

import com.jakewharton.fliptables.FlipTable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.LoggerFactory;

public class GrokFunctions {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static Grok getGrok(String grokExpr) throws GrokException {
    Grok grok = new Grok();

    InputStream input = GrokFunctions.class.getResourceAsStream("/patterns/common");
    if(input != null) {
      grok.addPatternFromReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    if(grokExpr != null) {
      grok.addPatternFromReader(new StringReader("pattern " + grokExpr));
      grok.compile("%{pattern}");
    }

    return grok;
  }

  @Stellar( namespace="GROK"
          , name="EVAL"
          , description = "Evaluate a grok expression for a statement"
          , params = {
                "grokExpression - The grok expression to evaluate"
               ,"data - Either a data message or a list of data messages to evaluate using the grokExpression"
                     }
          ,returns="The Map associated with the grok expression being evaluated on the list of messages."
  )
  public static class Evaluate implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String grokExpression = (String) args.get(0);
      Object arg =  args.get(1);
      if(grokExpression == null || arg == null) {
        return null;
      }
      List<String> strs = null;
      if(arg instanceof List) {
        strs = (List<String>) arg;
      }
      else if(arg instanceof String) {
        strs = new ArrayList<>();
        strs.add((String)arg);
      }
      else {
        return null;
      }
      Grok grok = null;
      try {
        grok = getGrok(grokExpression);
      } catch (GrokException e) {
        LOG.error("unable to parse {}: {}", grokExpression, e.getMessage(), e);
        return null;
      }
      List<Map<String, Object>> outputMap = new ArrayList<>();
      Set<String> keys = new TreeSet<>();
      for(String str : strs) {
        Match m = grok.match(str);
        m.captures();
        Map<String, Object> ret = m.toMap();
        if (ret != null && ret.isEmpty()) {
          outputMap.add(new HashMap<>());
        } else {
          ret.remove("pattern");
          keys.addAll(ret.keySet());
          outputMap.add(ret);
        }
      }
      if(keys.isEmpty()){
        return "NO MATCH";
      }
      String[] headers = new String[keys.size()];
      String[][] data = new String[outputMap.size()][keys.size()];
      {
        int i = 0;
        for (String key : keys) {
          headers[i++] = key;
        }
      }
      int rowNum = 0;
      for(Map<String, Object> output : outputMap) {
        String[] row = new String[keys.size()];
        int colNum = 0;
        for(String key : keys) {
          row[colNum++] = "" + output.getOrDefault(key, "MISSING");
        }
        data[rowNum++] = row;
      }
      return FlipTable.of(headers, data);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar( namespace="GROK"
          , name="PREDICT"
          , description = "Discover a grok statement for an input doc"
          , params = {
               "data - The data to discover a grok expression from"
                     }
          ,returns="A grok expression that should match the data."
  )
  public static class Predict implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String str = (String) args.get(0);
      if(str == null) {
        return null;
      }
      Grok grok = null;
      try {
        grok = getGrok(null);
      } catch (GrokException e) {
        LOG.error("unable to construct grok object: {}", e.getMessage(), e);
        return null;
      }
      return grok.discover(str);
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
