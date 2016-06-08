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

package org.apache.metron.common.field.validation;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.FieldValidator;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Map;

public class BaseValidationTest {
  public Configurations getConfiguration(String config) throws IOException {
    Configurations configurations = new Configurations();
    configurations.updateGlobalConfig(Bytes.toBytes(config));
    return configurations;
  }

  public FieldValidator getValidator(Configurations configurations) throws IOException {
    return configurations.getFieldValidations().get(0);
  }

  public boolean execute(String config, Map<String, Object> input) throws IOException {
    Configurations configurations = getConfiguration(config);

    FieldValidator validator = getValidator(configurations);
    return validator.isValid(new JSONObject(input)
                                       ,configurations.getGlobalConfig()
                            );
  }
}
