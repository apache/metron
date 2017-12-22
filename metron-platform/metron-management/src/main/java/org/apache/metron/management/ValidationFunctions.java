/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.management;

import com.jakewharton.fliptables.FlipTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.validation.StellarZookeeperBasedValidator;
import org.apache.metron.stellar.common.utils.validation.ValidationResult;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

/**
 * The VALIDATE namespace is used for functions that validate Stellar rule strings.
 */
public class ValidationFunctions {

  private static final String[] headers = new String[]{"PATH", "RULE", "ERROR", "VALID"};

  @Stellar(namespace = "VALIDATE", name = "STELLAR_RULE_CONFIGS", description =
      "Attempts to validate deployed Stellar expressions by ensuring they compile."
          + "This is useful when expressions may have been valid when deployed, but may have been"
          + "invalidated by a language change after that", params = {
      "wrap : Optional. The Length of string to wrap the columns"
  }, returns = "A table of validation results")
  public static class DiscoverAndValidateStellarRules implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) {

      int wordWrap = -1;
      if(args.size() > 0) {
        wordWrap = ConversionUtils.convert(args.get(0), Integer.class);
      }

      final int wrapSize = wordWrap;
      Optional<Object> clientOpt = context.getCapability(Context.Capabilities.ZOOKEEPER_CLIENT);
      if (!clientOpt.isPresent()) {
        throw new IllegalStateException(
            "VALIDATE_STELLAR_RULE_CONFIGS requires zookeeper.  Please connect to zookeeper.");
      }
      CuratorFramework client = (CuratorFramework) clientOpt.get();

      StellarZookeeperBasedValidator validator = new StellarZookeeperBasedValidator(client);
      Iterable<ValidationResult> result = validator.validate(Optional.empty());

      final ArrayList<String[]> dataList = new ArrayList<>();
      result.forEach((v) -> {
        dataList.add(new String[]{
            toWrappedString(v.getRulePath(),wrapSize),
            toWrappedString(v.getRule(),wrapSize),
            toWrappedString(v.getError(),wrapSize),
            toWrappedString(String.valueOf(v.isValid()),wrapSize)
        });
      });

      if (dataList.isEmpty()) {
        return FlipTable.of(headers, new String[][]{});
      }

      String[][] data = new String[dataList.size()][headers.length];
      for (int i = 0; i < dataList.size(); ++i) {
        data[i] = dataList.get(i);
      }
      return FlipTable.of(headers, data);
    }

    private static String toWrappedString(Object o, int wrap) {
      if (o == null) {
        return "";
      }
      String s = "" + o;
      if (wrap <= 0) {
        return s;
      }
      return WordUtils.wrap(s, wrap);
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
