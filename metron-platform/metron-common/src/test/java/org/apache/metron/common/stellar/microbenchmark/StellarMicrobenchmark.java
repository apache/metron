/*
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
package org.apache.metron.common.stellar.microbenchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.io.Files;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.utils.JSONUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class StellarMicrobenchmark {

  public static int DEFAULT_WARMUP = 100;
  public static int DEFAULT_NUM_TIMES = 1000;
  public static void main(String... argv) throws IOException {
    List<String> lines = Files.readLines(new File(argv[0]), Charset.defaultCharset());
    Map<String, Object> variables = JSONUtils.INSTANCE.load(new FileInputStream(argv[1]), new TypeReference<Map<String, Object>>() {
    });
    PrintWriter out = new PrintWriter(System.out);
    if(argv.length > 2) {
      out = new PrintWriter(new File(argv[2]));
    }
    for(String statement : lines) {
      if(statement.trim().startsWith("#")) {
        continue;
      }
      Microbenchmark.StellarStatement s = new Microbenchmark.StellarStatement();
      s.context = Context.EMPTY_CONTEXT();
      s.expression = statement;
      s.functionResolver = StellarFunctions.FUNCTION_RESOLVER();
      s.variableResolver = k -> variables.get(k);
      DescriptiveStatistics stats = Microbenchmark.run(s, DEFAULT_WARMUP, DEFAULT_NUM_TIMES);
      out.println("Expression: " + statement);
      out.println(Microbenchmark.describe(stats, new double[] {50d, 75d, 95d, 99d}));
    }
    if(argv.length > 2) {
      out.close();
    }
  }
}
