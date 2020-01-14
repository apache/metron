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

package org.apache.metron.stellar.dsl.functions;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConversionFunctionsTest {

  @Test
  public void conversionFunctionsShouldProperlyConvertToSpecificType() {
    assertEquals(1D, new ConversionFunctions.TO_DOUBLE().apply(Collections.singletonList(1)));
    assertEquals(1F, new ConversionFunctions.TO_FLOAT().apply(Collections.singletonList(1.0D)));
    assertEquals(1, new ConversionFunctions.TO_INTEGER().apply(Collections.singletonList(1.0D)));
    assertEquals(1L, new ConversionFunctions.TO_LONG().apply(Collections.singletonList(1F)));
  }

  @Test
  public void conversionFunctionsShouldProperlyHandleNull() {
    assertNull(new ConversionFunctions.TO_DOUBLE().apply(Collections.singletonList(null)));
    assertNull(new ConversionFunctions.TO_FLOAT().apply(Collections.singletonList(null)));
    assertNull(new ConversionFunctions.TO_INTEGER().apply(Collections.singletonList(null)));
    assertNull(new ConversionFunctions.TO_LONG().apply(Collections.singletonList(null)));
  }
}
