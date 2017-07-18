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
package org.apache.metron.stellar.dsl.functions;

import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.common.system.Environment;

import java.util.List;
import java.util.function.Function;

public class SystemFunctions {

  @Stellar(namespace = "SYSTEM",
          name = "ENV_GET",
          description = "Returns the value associated with an environment variable",
          params = {
                  "env_var - Environment variable name to get the value for"
          },
          returns = "String"
  )
  public static class EnvGet extends BaseStellarFunction {
    private Environment env;

    public EnvGet() {
      this(new Environment());
    }

    public EnvGet(Environment env) {
      this.env = env;
    }

    @Override
    public Object apply(List<Object> args) {
      return extractTypeChecked(args, 0, String.class, x -> env.get((String) x.get(0)));
    }
  }

  /**
   * Extract type-checked value from an argument list using the specified type check and extraction function
   *
   * @param args Arguments to check
   * @param i Index of argument to extract
   * @param clazz Object type to verify
   * @param extractFunc Function applied to extract the value from args
   * @return value from args if passes type checks, null otherwise
   */
  public static Object extractTypeChecked(List<Object> args, int i, Class clazz, Function<List<Object>, Object> extractFunc) {
    if (args.size() < i + 1) {
      return null;
    } else if (clazz.isInstance(args.get(i))) {
      return extractFunc.apply(args);
    } else {
      return null;
    }
  }

  @Stellar(namespace = "SYSTEM",
          name = "PROPERTY_GET",
          description = "Returns the value associated with a Java system property",
          params = {
                  "key - Property to get the value for"
          },
          returns = "String"
  )
  public static class PropertyGet extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return extractTypeChecked(args, 0, String.class, x -> System.getProperty((String) args.get(0)));
    }
  }
}
