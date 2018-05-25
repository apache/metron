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

package org.apache.metron.common.field;

import org.apache.metron.common.utils.ReflectionUtils;

/**
 * Allows the field name converter to be specified using a short-hand
 * name, rather than the entire fully-qualified class name.
 */
public enum FieldNameConverters {

  /**
   * A {@link FieldNameConverter} that does not rename any fields.  All field
   * names remain unchanged.
   */
  NOOP(NoopFieldNameConverter.class.getName()),

  /**
   * A {@link FieldNameConverter} that replaces all field names containing dots
   * with colons.
   */
  DEDOT(DeDotFieldNameConverter.class.getName());

  /**
   * The name of a class that implements {@link FieldNameConverter}.
   */
  private String className;

  FieldNameConverters(String className) {
    this.className = className;
  }

  /**
   * Returns a new instance of the {@link FieldNameConverter}.
   */
  public FieldNameConverter get() {
    return ReflectionUtils.createInstance(className);
  }
}
