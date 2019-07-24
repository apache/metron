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
package org.apache.metron.enrichment.converter;

import java.lang.reflect.InvocationTargetException;

/**
 * The default implementation of an {@link EnrichmentConverterFactory}.
 */
public class DefaultEnrichmentConverterFactory implements EnrichmentConverterFactory {

  /**
   * The fully-qualified class name of an {@link EnrichmentConverter}.
   */
  private String className;

  public DefaultEnrichmentConverterFactory() {
    this.className = EnrichmentConverter.class.getName();
  }

  public DefaultEnrichmentConverterFactory(String className) {
    this.className = className;
  }

  @Override
  public EnrichmentConverter create(String tableName) {
    try {
      Class<? extends EnrichmentConverter> clazz = (Class<? extends EnrichmentConverter>) Class.forName(className);
      return clazz.getConstructor().newInstance();

    } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
      String msg = String.format("Unable to instantiate EnrichmentConverter; className=%s", className);
      throw new IllegalStateException(msg, e);
    }
  }
}
