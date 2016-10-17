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

import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.system.Environment;

import java.util.List;

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
      if (args.size() == 0) {
        return null;
      } else {
        return env.get((String) args.get(0));
      }
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
      if (args.size() == 0) {
        return null;
      } else {
        return System.getProperty((String) args.get(0));
      }
    }
  }
}
