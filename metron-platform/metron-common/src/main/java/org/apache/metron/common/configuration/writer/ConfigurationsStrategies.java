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
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.zookeeper.configurations.ConfigurationsUpdater;
import org.apache.metron.common.zookeeper.configurations.EnrichmentUpdater;
import org.apache.metron.common.zookeeper.configurations.IndexingUpdater;
import org.apache.metron.common.zookeeper.configurations.ParserUpdater;
import org.apache.metron.common.zookeeper.configurations.ProfilerUpdater;
import org.apache.metron.common.zookeeper.configurations.Reloadable;

/**
 * Strategy pattern implementation that couples factories for WriterConfiguration and
 * ConfigurationsUpdater together for a particular type.
 * <br>
 * <strong>Note:</strong> The enum type definition does not specify generics because
 * enums are disallowed from doing this in Java.
 */
public enum ConfigurationsStrategies implements ConfigurationStrategy {

  PARSERS(new ConfigurationStrategy<ParserConfigurations>() {

    @Override
    public WriterConfiguration createWriterConfig(BulkMessageWriter writer,
        Configurations configs) {
      if (configs instanceof ParserConfigurations) {
        return new ParserWriterConfiguration((ParserConfigurations) configs);
      } else {
        throw new IllegalArgumentException(
            "Expected config of type ParserConfigurations but found " + configs.getClass());
      }
    }

    @Override
    public ConfigurationsUpdater<ParserConfigurations> createUpdater(Reloadable reloadable,
        Supplier configSupplier) {
      return new ParserUpdater(reloadable, configSupplier);
    }
  }),

  ENRICHMENT(new ConfigurationStrategy() {

    @Override
    public WriterConfiguration createWriterConfig(BulkMessageWriter writer,
        Configurations configs) {
      if (configs instanceof EnrichmentConfigurations) {
        return new EnrichmentWriterConfiguration((EnrichmentConfigurations) configs);
      } else {
        throw new IllegalArgumentException(
            "Expected config of type EnrichmentConfigurations but found " + configs.getClass());
      }
    }

    @Override
    public ConfigurationsUpdater<EnrichmentConfigurations> createUpdater(Reloadable reloadable,
        Supplier configSupplier) {
      return new EnrichmentUpdater(reloadable, configSupplier);
    }
  }),

  INDEXING(new ConfigurationStrategy() {

    @Override
    public WriterConfiguration createWriterConfig(BulkMessageWriter writer,
        Configurations configs) {
      if (configs instanceof IndexingConfigurations) {
        return new IndexingWriterConfiguration(writer.getName(), (IndexingConfigurations) configs);
      } else {
        throw new IllegalArgumentException(
            "Expected config of type IndexingConfigurations but found " + configs.getClass());
      }
    }

    @Override
    public ConfigurationsUpdater<IndexingConfigurations> createUpdater(Reloadable reloadable,
        Supplier configSupplier) {
      return new IndexingUpdater(reloadable, configSupplier);
    }
  }),

  PROFILER(new ConfigurationStrategy() {

    @Override
    public WriterConfiguration createWriterConfig(BulkMessageWriter writer,
        Configurations configs) {
        if (configs instanceof ProfilerConfigurations) {
          return new ProfilerWriterConfiguration((ProfilerConfigurations) configs);
        } else {
          throw new IllegalArgumentException(
              "Expected config of type IndexingConfigurations but found " + configs.getClass());
        }
    }

    @Override
    public ConfigurationsUpdater createUpdater(Reloadable reloadable, Supplier configSupplier) {
      return new ProfilerUpdater(reloadable, configSupplier);
    }
  });

  private ConfigurationStrategy<? extends Configurations> strategy;

  ConfigurationsStrategies(ConfigurationStrategy<? extends Configurations> strategy) {
    this.strategy = strategy;
  }

  @Override
  public WriterConfiguration createWriterConfig(BulkMessageWriter writer, Configurations configs) {
    return strategy.createWriterConfig(writer, configs);
  }

  /**
   * Config updater.
   * @param reloadable callback
   * @param configSupplier Supplier provides config of type {@code <? extends Configurations>}
   * @return Config updater
   */
  @Override
  public ConfigurationsUpdater createUpdater(Reloadable reloadable, Supplier configSupplier) {
    return strategy.createUpdater(reloadable, configSupplier);
  }
}
