package org.apache.metron.dataloads.nonbulk.flatfile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.hbase.mr.BulkLoadMapper;
import org.apache.metron.enrichment.converter.EnrichmentConverter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public enum MapReduceImporter implements Importer{
  INSTANCE
  ;

  @Override
  public void importData(EnumMap<LoadOptions, Optional<Object>> config
                        , ExtractorHandler handler
                        , Configuration hadoopConfig
                        ) throws IOException {
    String table = (String) config.get(LoadOptions.HBASE_TABLE).get();
    String cf = (String) config.get(LoadOptions.HBASE_CF).get();
    String extractorConfigContents  = (String) config.get(LoadOptions.EXTRACTOR_CONFIG).get();
    Job job = new Job(hadoopConfig);
    List<String> inputs = (List<String>) config.get(LoadOptions.INPUT).get();
    job.setJobName("MapReduceImporter: " + inputs.stream().collect(Collectors.joining(",")) + " => " +  table + ":" + cf);
    System.out.println("Configuring " + job.getJobName());
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
    handler.getInputFormatHandler().set(job, paths, handler.getConfig());
    try {
      job.waitForCompletion(true);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to complete job: " + e.getMessage(), e);
    }
  }
}
