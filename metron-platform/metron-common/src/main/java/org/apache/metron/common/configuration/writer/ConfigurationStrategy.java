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
package org.apache.metron.common.configuration.writer;

import java.util.function.Supplier;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.zookeeper.configurations.ConfigurationsUpdater;
import org.apache.metron.common.zookeeper.configurations.Reloadable;

public interface ConfigurationStrategy<T extends Configurations> {

  /**
   * Create a specific writer configuration.
   * @param writer provided for the underlying creator to access metadata as needed
   * @param configs a {@link WriterConfiguration} will typically access the pass configs
   * @return A {@link WriterConfiguration} created from the configs
   */
  WriterConfiguration createWriterConfig(BulkMessageWriter writer, Configurations configs);

  /**
   * Create specific {@link ConfigurationsUpdater} for the type of config extending Configurations.
   * @param reloadable setup as a callback by the updater
   * @param configSupplier supplies config to the updater
   * @return updater
   */
  ConfigurationsUpdater<T> createUpdater(Reloadable reloadable, Supplier<T> configSupplier);

}
