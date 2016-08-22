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

public enum StellarFunctions implements StellarFunction {
  TO_LOWER(new StringFunctions.ToLower())
  ,TO_UPPER(new StringFunctions.ToUpper())
  ,TO_STRING(new StringFunctions.ToString())
  ,TO_INTEGER(new ConversionFunctions.Cast<>(Integer.class))
  ,TO_DOUBLE(new ConversionFunctions.Cast<>(Double.class))
  ,TRIM(new StringFunctions.Trim())
  ,JOIN(new StringFunctions.JoinFunction())
  ,SPLIT(new StringFunctions.SplitFunction())
  ,GET_FIRST(new StringFunctions.GetFirst())
  ,GET_LAST(new StringFunctions.GetLast())
  ,GET(new StringFunctions.Get())
  ,MAP_GET(new MapFunctions.MapGet())
  ,DOMAIN_TO_TLD(new NetworkFunctions.ExtractTLD())
  ,DOMAIN_REMOVE_TLD(new NetworkFunctions.RemoveTLD())
  ,DOMAIN_REMOVE_SUBDOMAINS(new NetworkFunctions.RemoveSubdomains())
  ,URL_TO_HOST(new NetworkFunctions.URLToHost())
  ,URL_TO_PORT(new NetworkFunctions.URLToPort())
  ,URL_TO_PATH(new NetworkFunctions.URLToPath())
  ,URL_TO_PROTOCOL(new NetworkFunctions.URLToProtocol())
  ,TO_EPOCH_TIMESTAMP(new DateFunctions.ToTimestamp())
  ,PROTOCOL_TO_NAME(new IPProtocolTransformation())
  ,IS_EMPTY ( new DataStructureFunctions.IsEmpty())
  ,IN_SUBNET( new NetworkFunctions.InSubnet())
  ,STARTS_WITH( new StringFunctions.StartsWith())
  ,ENDS_WITH( new StringFunctions.EndsWith())
  ,REGEXP_MATCH( new StringFunctions.RegexpMatch())
  , IS_IP(new Predicate2Transformation(new IPValidation()))
  , IS_DOMAIN(new Predicate2Transformation(new DomainValidation()))
  , IS_EMAIL(new Predicate2Transformation(new EmailValidation()))
  , IS_URL(new Predicate2Transformation(new URLValidation()))
  , IS_DATE(new Predicate2Transformation(new DateValidation()))
  , IS_INTEGER(new Predicate2Transformation(new IntegerValidation()))
  , MAP_EXISTS( new MapFunctions.MapExists())
  , MAAS_GET_ENDPOINT( new MaaSFunctions.GetEndpoint())
  , MODEL_APPLY(new MaaSFunctions.ModelApply())
  ;
  private static class Predicate2Transformation extends BaseStellarFunction {
    Predicate<List<Object>> pred;
    public Predicate2Transformation(Predicate<List<Object>> pred) {
      this.pred = pred;
    }

    @Override
    public Object apply(List<Object> objects) {
      return pred.test(objects);
    }
  }
  StellarFunction func;
  StellarFunctions(StellarFunction func) {
    this.func = func;
  }



  @Override
  public Object apply(List<Object> input, Context context) {
    return func.apply(input, context);
  }
  @Override
  public void initialize(Context context) {
    func.initialize(context);
  }

  public static FunctionResolver FUNCTION_RESOLVER() {
    return new FunctionResolver() {
      @Override
      public void initializeFunctions(Context context) {
        for(StellarFunctions s : StellarFunctions.values()) {
          s.initialize(context);
        }
      }

      @Override
      public StellarFunction apply(String s) {
        StellarFunctions func  = null;
        try {
          func = StellarFunctions.valueOf(s);
          return func;
        }
        catch(Exception e) {
          throw new IllegalStateException("Unable to resolve function " + s);
        }
      }
    };

  }
}
