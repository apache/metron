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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.nonbulk.flatfile.HBaseExtractorState;
import org.apache.metron.dataloads.nonbulk.flatfile.LoadOptions;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.hbase.HTableProvider;

import java.io.*;
import java.util.*;

public class LocalImporter extends AbstractLocalImporter<LoadOptions, HBaseExtractorState> {

  public interface HTableProviderRetriever {
    HTableProvider retrieve();
  }

  HTableProviderRetriever provider;

  public LocalImporter(HTableProviderRetriever provider) {
    this.provider = provider;
  }

  public LocalImporter() {
    this(() -> new HTableProvider());
  }


  @Override
  protected List<String> getInputs(EnumMap<LoadOptions, Optional<Object>> config) {
    return (List<String>) config.get(LoadOptions.INPUT).get();
  }

  @Override
  protected boolean isQuiet(EnumMap<LoadOptions, Optional<Object>> config) {
    return (boolean) config.get(LoadOptions.QUIET).get();
  }

  @Override
  protected int batchSize(EnumMap<LoadOptions, Optional<Object>> config) {
    return (int) config.get(LoadOptions.BATCH_SIZE).get();
  }

  @Override
  protected int numThreads(EnumMap<LoadOptions, Optional<Object>> config, ExtractorHandler handler) {
    return (int) config.get(LoadOptions.NUM_THREADS).get();
  }

  @Override
  protected void validateState(EnumMap<LoadOptions, Optional<Object>> config, ExtractorHandler handler) {
    assertOption(config, LoadOptions.HBASE_CF);
    assertOption(config, LoadOptions.HBASE_TABLE);
  }



  @Override
  protected ThreadLocal<HBaseExtractorState> createState(EnumMap<LoadOptions, Optional<Object>> config
                                                   , Configuration hadoopConfig
                                                   , final ExtractorHandler handler
                                                   ) {
    ThreadLocal<HBaseExtractorState> state = new ThreadLocal<HBaseExtractorState>() {
      @Override
      protected HBaseExtractorState initialValue() {
        try {
          String cf = (String) config.get(LoadOptions.HBASE_CF).get();
          HTableInterface table = provider.retrieve().getTable(hadoopConfig, (String) config.get(LoadOptions.HBASE_TABLE).get());
          return new HBaseExtractorState(table, cf, handler.getExtractor(), new EnrichmentConverter(), hadoopConfig);
        } catch (IOException e1) {
          throw new IllegalStateException("Unable to get table: " + e1);
        }
      }
    };
    return state;
  }

  @Override
  protected void extract(HBaseExtractorState state, String line) throws IOException {
    HBaseExtractorState es = state;
    es.getTable().put(toPut(line, es.getExtractor(), state.getCf(), es.getConverter()));
  }

  public List<Put> toPut(String line
                     , Extractor extractor
                     , String cf
                     , HbaseConverter converter
                     ) throws IOException
  {
    List<Put> ret = new ArrayList<>();
    Iterable<LookupKV> kvs = extractor.extract(line);
    for(LookupKV kv : kvs) {
      Put put = converter.toPut(cf, kv.getKey(), kv.getValue());
      ret.add(put);
    }

    return ret;
  }

}
