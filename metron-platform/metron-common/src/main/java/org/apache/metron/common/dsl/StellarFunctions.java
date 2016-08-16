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

package org.apache.metron.common.dsl;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.metron.common.dsl.functions.*;
import org.apache.metron.common.field.transformation.IPProtocolTransformation;
import org.apache.metron.common.field.validation.network.DomainValidation;
import org.apache.metron.common.field.validation.network.EmailValidation;
import org.apache.metron.common.field.validation.network.IPValidation;
import org.apache.metron.common.field.validation.network.URLValidation;
import org.apache.metron.common.field.validation.primitive.DateValidation;
import org.apache.metron.common.field.validation.primitive.IntegerValidation;
import org.apache.metron.common.utils.ConversionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Functions that are exposed to the Stellar environment.
 */
public enum StellarFunctions implements Function<List<Object>, Object> {

  // string functions
  TO_LOWER(StringFunctions.ToLower),
  TO_UPPER(StringFunctions.ToUpper),
  TRIM(StringFunctions.Trim),
  JOIN(StringFunctions.Join),
  SPLIT(StringFunctions.Split),
  STARTS_WITH(StringFunctions.StartsWith),
  ENDS_WITH(StringFunctions.EndsWith),
  REGEXP_MATCH(StringFunctions.RegexMatch),

  // list functions
  GET_FIRST(ListFunctions.GetFirst),
  GET_LAST(ListFunctions.GetLast),
  GET(ListFunctions.Get),
  IS_EMPTY (ListFunctions.IsEmpty),

  // type conversions
  TO_STRING(ConversionFunctions.ToString),
  TO_INTEGER(ConversionFunctions.ToInteger),
  TO_DOUBLE(ConversionFunctions.ToDouble),

  // map functions
  MAP_GET(MapFunctions.MapGet),
  MAP_EXISTS(MapFunctions.MapExists),

  // network functions
  DOMAIN_TO_TLD(NetworkFunctions.ExtractTld),
  DOMAIN_REMOVE_TLD(NetworkFunctions.RemoveTld),
  DOMAIN_REMOVE_SUBDOMAINS(NetworkFunctions.RemoveSubdomains),
  URL_TO_HOST(NetworkFunctions.UrlToHost),
  URL_TO_PORT(NetworkFunctions.UrlToPort),
  URL_TO_PATH(NetworkFunctions.UrlToPath),
  URL_TO_PROTOCOL(NetworkFunctions.UrlToProtocol),
  IN_SUBNET(NetworkFunctions.InSubnet),
  PROTOCOL_TO_NAME(new IPProtocolTransformation()),

  // date functions
  TO_EPOCH_TIMESTAMP(new DateFunctions.ToTimestamp()),

  // validation functions
  IS_IP(new Predicate2Transformation(new IPValidation())),
  IS_DOMAIN(new Predicate2Transformation(new DomainValidation())),
  IS_EMAIL(new Predicate2Transformation(new EmailValidation())),
  IS_URL(new Predicate2Transformation(new URLValidation())),
  IS_DATE(new Predicate2Transformation(new DateValidation())),
  IS_INTEGER(new Predicate2Transformation(new IntegerValidation()));

  /**
   * The function to apply.
   */
  Function<List<Object>, Object> func;

  StellarFunctions(Function<List<Object>, Object> func) {
    this.func = func;
  }

  /**
   * Apply the function.
   * @param input The arguments to the function.
   */
  @Override
  public Object apply(List<Object> input) {
    return func.apply(input);
  }

  /**
   * An adapter class that allows a Predicate to be applied as a Function.
   */
  private static class Predicate2Transformation implements Function<List<Object>, Object> {

    Predicate<List<Object>> pred;

    public Predicate2Transformation(Predicate<List<Object>> pred) {
      this.pred = pred;
    }

    @Override
    public Object apply(List<Object> objects) {
      return pred.test(objects);
    }
  }
}
