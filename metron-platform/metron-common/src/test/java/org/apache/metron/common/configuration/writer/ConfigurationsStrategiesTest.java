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

import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.zookeeper.configurations.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.apache.metron.common.configuration.writer.ConfigurationsStrategies.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigurationsStrategiesTest {

  @Mock
  private BulkMessageWriter writer;
  @Mock
  private Reloadable reloadable;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void strategies_build_writer_configs() {
    assertThat(PARSERS.createWriterConfig(writer, new ParserConfigurations()),
        instanceOf(ParserWriterConfiguration.class));
    assertThat(ENRICHMENT.createWriterConfig(writer, new EnrichmentConfigurations()),
        instanceOf(EnrichmentWriterConfiguration.class));
    assertThat(INDEXING.createWriterConfig(writer, new IndexingConfigurations()),
        instanceOf(IndexingWriterConfiguration.class));
    assertThat(PROFILER.createWriterConfig(writer, new ProfilerConfigurations()),
        instanceOf(ProfilerWriterConfiguration.class));
  }

  @Test
  public void strategies_build_updaters() {
    assertThat(PARSERS.createUpdater(reloadable, ParserConfigurations::new),
        instanceOf(ParserUpdater.class));
    assertThat(ENRICHMENT.createUpdater(reloadable, EnrichmentConfigurations::new),
        instanceOf(EnrichmentUpdater.class));
    assertThat(INDEXING.createUpdater(reloadable, IndexingConfigurations::new),
        instanceOf(IndexingUpdater.class));
    assertThat(PROFILER.createUpdater(reloadable, ProfilerConfigurations::new),
        instanceOf(ProfilerUpdater.class));
  }

}
