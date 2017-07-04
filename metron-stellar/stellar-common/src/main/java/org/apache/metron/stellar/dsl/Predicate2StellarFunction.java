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
package org.apache.metron.stellar.dsl;

import java.util.List;
import java.util.function.Predicate;

public class Predicate2StellarFunction extends BaseStellarFunction {
  Predicate<List<Object>> pred;
  public Predicate2StellarFunction(Predicate<List<Object>> pred) {
    this.pred = pred;
  }

  @Override
  public Object apply(List<Object> objects) {
    return pred.test(objects);
  }
}
