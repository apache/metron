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
package org.apache.metron.indexing.mutation;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.function.Supplier;

public class Mutation implements Serializable {
  MutationOperation mutator;
  String mutationArg;

  public Mutation(MutationOperation mutator, String mutationArg) {
    this.mutator = mutator;
    this.mutationArg = mutationArg;
  }

  public static Mutation of(MutationOperation mutator, String mutationArg) {
    return new Mutation(mutator, mutationArg);
  }

  /**
   * Applies this function to the given argument.
   *
   * @param jsonNodeSupplier the function argument
   * @return the function result
   */
  public String apply(Supplier<JsonNode> jsonNodeSupplier) throws MutationException {
    return mutator.mutate(jsonNodeSupplier, mutationArg);
  }

  public MutationOperation getMutator() {
    return mutator;
  }

  public void setMutator(MutationOperation mutator) {
    this.mutator = mutator;
  }

  public String getMutationArg() {
    return mutationArg;
  }

  public void setMutationArg(String mutationArg) {
    this.mutationArg = mutationArg;
  }
}
