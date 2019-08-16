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
package org.apache.metron.dataloads.nonbulk.flatfile.importer;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.dataloads.extractor.ExtractorCapabilities;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.StatefulExtractor;
import org.apache.metron.dataloads.nonbulk.flatfile.SummarizeOptions;
import org.apache.metron.dataloads.nonbulk.flatfile.writer.InvalidWriterOutput;
import org.apache.metron.dataloads.nonbulk.flatfile.writer.Writer;
import org.apache.metron.dataloads.nonbulk.flatfile.writer.Writers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class LocalSummarizer extends AbstractLocalImporter<SummarizeOptions, LocalSummarizer.SummarizationState> {
  List<SummarizationState> stateList;

  public LocalSummarizer() {
    stateList = Collections.synchronizedList(new ArrayList<>());
  }

  public static class SummarizationState {
    AtomicReference<Object> state;
    StatefulExtractor extractor;
    public SummarizationState(StatefulExtractor extractor, Object initState) {
      this.state = new AtomicReference<>(initState);
      this.extractor = extractor;
    }

    public AtomicReference<Object> getState() {
      return state;
    }

    public StatefulExtractor getExtractor() {
      return extractor;
    }

  }

  @Override
  protected boolean isQuiet(EnumMap<SummarizeOptions, Optional<Object>> config) {
    return (boolean) config.getOrDefault(SummarizeOptions.QUIET, Optional.of(false)).get();
  }

  @Override
  protected int batchSize(EnumMap<SummarizeOptions, Optional<Object>> config) {
    return (int) config.getOrDefault(SummarizeOptions.BATCH_SIZE, Optional.of(1)).get();
  }

  @Override
  protected int numThreads(EnumMap<SummarizeOptions, Optional<Object>> config, ExtractorHandler handler) {
    if(handler.getExtractor().getCapabilities().contains(ExtractorCapabilities.MERGEABLE)) {
      return (int) config.get(SummarizeOptions.NUM_THREADS).get();
    }
    else {
      //force one thread in the case it's not mergeable.
      return 1;
    }
  }

  @Override
  protected void validateState(EnumMap<SummarizeOptions, Optional<Object>> config, ExtractorHandler handler) {
    if(!(handler.getExtractor() instanceof StatefulExtractor)){
      throw new IllegalStateException("Extractor must be a stateful extractor and " + handler.getExtractor().getClass().getName() + " is not.");
    }
    assertOption(config, SummarizeOptions.OUTPUT);
    if(!handler.getExtractor().getCapabilities().contains(ExtractorCapabilities.STATEFUL)) {
      throw new IllegalStateException("Unable to operate on a non-stateful extractor.  " +
              "If you have not specified \"stateUpdate\" in your Extractor config, there is nothing to do here and nothing will be written.");
    }

  }

  @Override
  protected ThreadLocal<SummarizationState> createState(EnumMap<SummarizeOptions, Optional<Object>> config, Configuration hadoopConfig, ExtractorHandler handler) {
    final StatefulExtractor extractor = (StatefulExtractor)handler.getExtractor();
    return ThreadLocal.withInitial(() -> {
      Object initState = extractor.initializeState(handler.getConfig());
      SummarizationState ret = new SummarizationState(extractor, initState);
      stateList.add(ret);
      return ret;
    });
  }


  @Override
  protected void extract(SummarizationState state, String line) throws IOException {
    state.getExtractor().extract(line, state.getState());
  }

  @Override
  public void importData(EnumMap<SummarizeOptions, Optional<Object>> config, ExtractorHandler handler, Configuration hadoopConfig) throws IOException, InvalidWriterOutput {
    Writer writer = (Writer) config.get(SummarizeOptions.OUTPUT_MODE).get();
    Optional<String> fileName = Optional.ofNullable((String)config.get(SummarizeOptions.OUTPUT).orElse(null));
    writer.validate(fileName, hadoopConfig);
    super.importData(config, handler, hadoopConfig);
    StatefulExtractor extractor = (StatefulExtractor) handler.getExtractor();
    Object finalState = null;
    if(stateList.size() == 1) {
      finalState = stateList.get(0).getState().get();
    }
    else if(stateList.size() > 1) {
      List<Object> states = new ArrayList<>();
      for(SummarizationState s : stateList) {
        states.add(s.getState().get());
      }
      finalState = extractor.mergeStates(states);
    }
    writer.write(finalState, fileName, hadoopConfig);
  }

  @Override
  protected List<String> getInputs(EnumMap<SummarizeOptions, Optional<Object>> config) {
    Object o = config.get(SummarizeOptions.INPUT).get();
    if(o == null) {
      return new ArrayList<>();
    }
    if(o instanceof String) {
      return ImmutableList.of((String)o);
    }
    return (List<String>) config.get(SummarizeOptions.INPUT).get();
  }
}
