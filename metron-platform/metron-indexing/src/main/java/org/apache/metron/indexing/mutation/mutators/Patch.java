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
package org.apache.metron.indexing.mutation.mutators;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonPatch;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.mutation.MutationException;
import org.apache.metron.indexing.mutation.Mutator;

import java.util.function.Supplier;

public class Patch implements Mutator {
  @Override
  public String mutate(Supplier<JsonNode> originalSupplier, String arg) throws MutationException {
    final JsonNode orig = originalSupplier.get();
    try {
      JsonNode out = JsonPatch.apply(JSONUtils.INSTANCE.load(arg, JsonNode.class), orig);
      return new String(JSONUtils.INSTANCE.toJSON(out));

    } catch (Exception e) {
      throw new MutationException("Unable to mutate: " + orig.asText() + " with " + arg + " because " + e.getMessage(), e);
    }
  }
}
