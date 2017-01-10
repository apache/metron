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

package org.apache.metron.pcap.mr;

import com.google.common.base.Joiner;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.log4j.Logger;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class PcapJob {
  private static final Logger LOG = Logger.getLogger(PcapJob.class);
  public static final String START_TS_CONF = "start_ts";
  public static final String END_TS_CONF = "end_ts";
  public static final String WIDTH_CONF = "width";
  public static class PcapPartitioner extends Partitioner<LongWritable, BytesWritable> implements Configurable {
    private Configuration configuration;
    Long start = null;
    Long end = null;
    Long width = null;
    @Override
    public int getPartition(LongWritable longWritable, BytesWritable bytesWritable, int numPartitions) {
      if(start == null) {
        initialize();
      }
      long x = longWritable.get();
      int ret = (int)Long.divideUnsigned(x - start, width);
      if(ret > numPartitions) {
        throw new IllegalArgumentException(String.format("Bad partition: key=%s, width=%d, partition=%d, numPartitions=%d"
                , Long.toUnsignedString(x), width, ret, numPartitions)
            );
      }
      return ret;
    }

    private void initialize() {
      start = Long.parseUnsignedLong(configuration.get(START_TS_CONF));
      end = Long.parseUnsignedLong(configuration.get(END_TS_CONF));
      width = Long.parseLong(configuration.get(WIDTH_CONF));
    }

    @Override
    public void setConf(Configuration conf) {
      this.configuration = conf;
    }

    @Override
    public Configuration getConf() {
      return configuration;
    }
  }
  public static class PcapMapper extends Mapper<LongWritable, BytesWritable, LongWritable, BytesWritable> {

    PcapFilter filter;
    long start;
    long end;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
      super.setup(context);
      filter = PcapFilters.valueOf(context.getConfiguration().get(PcapFilterConfigurator.PCAP_FILTER_NAME_CONF)).create();
      filter.configure(context.getConfiguration());
      start = Long.parseUnsignedLong(context.getConfiguration().get(START_TS_CONF));
      end = Long.parseUnsignedLong(context.getConfiguration().get(END_TS_CONF));
    }

    @Override
    protected void map(LongWritable key, BytesWritable value, Context context) throws IOException, InterruptedException {
      if (Long.compareUnsigned(key.get(), start) >= 0 && Long.compareUnsigned(key.get(), end) <= 0) {
        // It is assumed that the passed BytesWritable value is always a *single* PacketInfo object. Passing more than 1
        // object will result in the whole set being passed through if any pass the filter. We cannot serialize PacketInfo
        // objects back to byte arrays, otherwise we could support more than one packet.
        // Note: short-circuit findAny() func on stream
        boolean send = filteredPacketInfo(value).findAny().isPresent();
        if (send) {
          context.write(key, value);
        }
      }
    }

    private Stream<PacketInfo> filteredPacketInfo(BytesWritable value) throws IOException {
      return PcapHelper.toPacketInfo(value.copyBytes()).stream().filter(filter);
    }
  }

  public static class PcapReducer extends Reducer<LongWritable, BytesWritable, LongWritable, BytesWritable> {
    @Override
    protected void reduce(LongWritable key, Iterable<BytesWritable> values, Context context) throws IOException, InterruptedException {
      for(BytesWritable value : values) {
        context.write(key, value);
      }
    }
  }

  protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
    List<Path> ret = new ArrayList<>();
    RemoteIterator<LocatedFileStatus> filesIt = fs.listFiles(basePath, true);
    while(filesIt.hasNext()){
      ret.add(filesIt.next().getPath());
    }
    return ret;
  }

  public Iterable<String> getPaths(FileSystem fs, Path basePath, long begin, long end) throws IOException {
    List<String> ret = new ArrayList<>();
    Iterator<Path> files = listFiles(fs, basePath).iterator();
    /*
    The trick here is that we need a trailing left endpoint, because we only capture the start of the
    timeseries kept in the file.
     */
    boolean isFirst = true;
    Path leftEndpoint = files.hasNext()?files.next():null;
    if(leftEndpoint == null) {
      return ret;
    }
    {
      Long ts = PcapHelper.getTimestamp(leftEndpoint.getName());
      if(ts != null && Long.compareUnsigned(ts, begin) >= 0 && Long.compareUnsigned(ts, end) <= 0) {
        ret.add(leftEndpoint.toString());
        isFirst = false;
      }
    }
    while(files.hasNext()) {
      Path p = files.next();
      Long ts = PcapHelper.getTimestamp(p.getName());
      if(ts != null && Long.compareUnsigned(ts, begin) >= 0 && Long.compareUnsigned(ts, end) <= 0) {
        if(isFirst && leftEndpoint != null) {
          ret.add(leftEndpoint.toString());
        }
        if(isFirst) {
          isFirst = false;
        }
        ret.add(p.toString());
      }
      else {
        leftEndpoint = p;
      }
    }
    if(LOG.isDebugEnabled()) {
      LOG.debug("Including files " + Joiner.on(",").join(ret));
    }
    return ret;
  }

  /**
   * Returns a lazily-read Iterable over a set of sequence files
   */
  private SequenceFileIterable readResults(Path outputPath, Configuration config, FileSystem fs) throws IOException {
    List<Path> files = new ArrayList<>();
    for (RemoteIterator<LocatedFileStatus> it = fs.listFiles(outputPath, false); it.hasNext(); ) {
      Path p = it.next().getPath();
      if (p.getName().equals("_SUCCESS")) {
        fs.delete(p, false);
        continue;
      }
      files.add(p);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(outputPath);
    }
    return new SequenceFileIterable(files, config);
  }

  public <T> SequenceFileIterable query(Path basePath
                            , Path baseOutputPath
                            , long beginNS
                            , long endNS
                            , int numReducers
                            , T fields
                            , Configuration conf
                            , FileSystem fs
                            , PcapFilterConfigurator<T> filterImpl
                            ) throws IOException, ClassNotFoundException, InterruptedException {
    String fileName = Joiner.on("_").join(beginNS, endNS, filterImpl.queryToString(fields), UUID.randomUUID().toString());
    if(LOG.isDebugEnabled()) {
      DateFormat format = SimpleDateFormat.getDateTimeInstance( SimpleDateFormat.LONG
                                                              , SimpleDateFormat.LONG
                                                              );
      String from = format.format(new Date(Long.divideUnsigned(beginNS, 1000000)));
      String to = format.format(new Date(Long.divideUnsigned(endNS, 1000000)));
      LOG.debug("Executing query " + filterImpl.queryToString(fields) + " on timerange " + from + " to " + to);
    }
    Path outputPath =  new Path(baseOutputPath, fileName);
    Job job = createJob( basePath
                       , outputPath
                       , beginNS
                       , endNS
                       , numReducers
                       , fields
                       , conf
                       , fs
                       , filterImpl
                       );
    boolean completed = job.waitForCompletion(true);
    if(completed) {
      return readResults(outputPath, conf, fs);
    }
    else {
      throw new RuntimeException("Unable to complete query due to errors.  Please check logs for full errors.");
    }
  }

  public static long findWidth(long start, long end, int numReducers) {
    return Long.divideUnsigned(end - start, numReducers) + 1;
  }

  public <T> Job createJob( Path basePath
                      , Path outputPath
                      , long beginNS
                      , long endNS
                      , int numReducers
                      , T fields
                      , Configuration conf
                      , FileSystem fs
                      , PcapFilterConfigurator<T> filterImpl
                      ) throws IOException
  {
    conf.set(START_TS_CONF, Long.toUnsignedString(beginNS));
    conf.set(END_TS_CONF, Long.toUnsignedString(endNS));
    conf.set(WIDTH_CONF, "" + findWidth(beginNS, endNS, numReducers));
    filterImpl.addToConfig(fields, conf);
    Job job = Job.getInstance(conf);
    job.setJarByClass(PcapJob.class);
    job.setMapperClass(PcapJob.PcapMapper.class);
    job.setMapOutputKeyClass(LongWritable.class);
    job.setMapOutputValueClass(BytesWritable.class);
    job.setNumReduceTasks(numReducers);
    job.setReducerClass(PcapReducer.class);
    job.setPartitionerClass(PcapPartitioner.class);
    job.setOutputKeyClass(LongWritable.class);
    job.setOutputValueClass(BytesWritable.class);
    SequenceFileInputFormat.addInputPaths(job, Joiner.on(',').join(getPaths(fs, basePath, beginNS, endNS )));
    job.setInputFormatClass(SequenceFileInputFormat.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    SequenceFileOutputFormat.setOutputPath(job, outputPath);
    return job;

  }
}
