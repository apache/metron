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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.log4j.Logger;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.hbase.mr.BulkLoadMapper;
import org.apache.metron.dataloads.nonbulk.flatfile.LoadOptions;
import org.apache.metron.enrichment.converter.EnrichmentConverter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public enum MapReduceImporter implements Importer{
  INSTANCE
  ;

  private static final Logger LOG = Logger.getLogger(MapReduceImporter.class);

  @Override
  public void importData(EnumMap<LoadOptions, Optional<Object>> config
                        , ExtractorHandler handler
                        , Configuration hadoopConfig
                        ) throws IOException {
    String table = (String) config.get(LoadOptions.HBASE_TABLE).get();
    String cf = (String) config.get(LoadOptions.HBASE_CF).get();
    String extractorConfigContents  = (String) config.get(LoadOptions.EXTRACTOR_CONFIG).get();
    Job job = Job.getInstance(hadoopConfig);
    List<String> inputs = (List<String>) config.get(LoadOptions.INPUT).get();
    job.setJobName("MapReduceImporter: " + inputs.stream().collect(Collectors.joining(",")) + " => " +  table + ":" + cf);
    LOG.info("Configuring " + job.getJobName());
    job.setJarByClass(MapReduceImporter.class);
    job.setMapperClass(org.apache.metron.dataloads.hbase.mr.BulkLoadMapper.class);
    job.setOutputFormatClass(TableOutputFormat.class);
    job.getConfiguration().set(TableOutputFormat.OUTPUT_TABLE, table);
    job.getConfiguration().set(BulkLoadMapper.COLUMN_FAMILY_KEY, cf);
    job.getConfiguration().set(BulkLoadMapper.CONFIG_KEY, extractorConfigContents);
    job.getConfiguration().set(BulkLoadMapper.CONVERTER_KEY, EnrichmentConverter.class.getName());
    job.setOutputKeyClass(ImmutableBytesWritable.class);
    job.setOutputValueClass(Put.class);
    job.setNumReduceTasks(0);
    List<Path> paths = inputs.stream().map(p -> new Path(p)).collect(Collectors.toList());
    handler.getInputFormat().set(job, paths, handler.getConfig());
    TableMapReduceUtil.initCredentials(job);
    try {
      job.waitForCompletion(true);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to complete job: " + e.getMessage(), e);
    }
  }
}
