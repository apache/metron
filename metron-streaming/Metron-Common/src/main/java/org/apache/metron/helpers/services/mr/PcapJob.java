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

package org.apache.metron.helpers.services.mr;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.metron.Constants;
import org.apache.metron.pcap.PcapParser;
import org.apache.metron.spout.pcap.PcapFileHelper;

import java.io.IOException;
import java.util.*;

public class PcapJob {
  public static class PcapMapper extends Mapper<LongWritable, BytesWritable, LongWritable, BytesWritable> {
    public static final String START_TS_CONF = "start_ts";
    public static final String END_TS_CONF = "end_ts";
    PcapParser parser;
    PcapFilter filter;
    long start;
    long end;
    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
      super.setup(context);
      parser = new PcapParser();
      parser.init();
      filter = new PcapFilter(context.getConfiguration());
      start = Long.parseUnsignedLong(context.getConfiguration().get(START_TS_CONF));
      end = Long.parseUnsignedLong(context.getConfiguration().get(END_TS_CONF));
    }

    @Override
    protected void map(LongWritable key, BytesWritable value, Context context) throws IOException, InterruptedException {
      if(key.get() >= start && key.get() <= end) {
        boolean send = Iterables.size(Iterables.filter(parser.getPacketInfo(value.getBytes()), filter)) > 0;
        if(send) {
          context.write(key, value);
        }
      }
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

  private Iterable<String> getPaths(FileSystem fs, Path basePath, long begin, long end) throws IOException {
    List<String> ret = new ArrayList<>();
    RemoteIterator<LocatedFileStatus> files = fs.listFiles(basePath, true);
    /*
    The trick here is that we need a trailing left endpoint, because we only capture the start of the
    timeseries kept in the file.
     */
    boolean isFirst = true;
    Path leftEndpoint = files.hasNext()?files.next().getPath():null;
    {
      Long ts = PcapFileHelper.getTimestamp(leftEndpoint.getName());
      if(ts != null && ts >= begin && ts <= end) {
        ret.add(leftEndpoint.toString());
        isFirst = false;
      }
    }
    while(files.hasNext()) {
      Path p = files.next().getPath();
      Long ts = PcapFileHelper.getTimestamp(p.getName());
      if(ts != null && ts >= begin && ts <= end) {
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
    return ret;
  }

  private List<byte[]> readResults(Path outputPath, Configuration config, FileSystem fs) throws IOException {
    List<byte[]> ret = new ArrayList<>();
    for(RemoteIterator<LocatedFileStatus> it= fs.listFiles(outputPath, false);it.hasNext();) {
      Path p = it.next().getPath();
      if(p.getName().equals("_SUCCESS")) {
        fs.delete(p, false);
        continue;
      }
      SequenceFile.Reader reader = new SequenceFile.Reader(config,
            SequenceFile.Reader.file(p));
      LongWritable key = new LongWritable();
      BytesWritable value = new BytesWritable();
      while(reader.next(key, value)) {
        byte[] data = new byte[value.getBytes().length];
        System.arraycopy(value.getBytes(), 0, data, 0, data.length);
        ret.add(data);
      }
      reader.close();
      fs.delete(p, false);
    }
    fs.delete(outputPath, false);
    return ret;
  }

  private static String queryToString(EnumMap<Constants.Fields, String> fields) {
    return Joiner.on("_").join(fields.values());
  }

  public List<byte[]> query(Path basePath
                            , Path baseOutputPath
                            , long beginNS
                            , long endNS
                            , EnumMap<Constants.Fields, String> fields
                            , Configuration conf
                            , FileSystem fs
                            ) throws IOException, ClassNotFoundException, InterruptedException {
    String fileName = Joiner.on("_").join(beginNS, endNS, queryToString(fields), UUID.randomUUID().toString());
    Path outputPath =  new Path(baseOutputPath, fileName);
    Job job = createJob( basePath
                       , outputPath
                       , beginNS
                       , endNS
                       ,fields
                       , conf
                       , fs
                       );
    boolean completed = job.waitForCompletion(true);
    if(completed) {
      return readResults(outputPath, conf, fs);
    }
    else {
      throw new RuntimeException("Unable to complete query due to errors.  Please check logs for full errors.");
    }
  }


  public Job createJob( Path basePath
                      , Path outputPath
                      , long beginNS
                      , long endNS
                      , EnumMap<Constants.Fields, String> fields
                      , Configuration conf
                      , FileSystem fs
                      ) throws IOException
  {
    conf.set(PcapMapper.START_TS_CONF, Long.toUnsignedString(beginNS));
    conf.set(PcapMapper.END_TS_CONF, Long.toUnsignedString(endNS));
    for(Map.Entry<Constants.Fields, String> kv : fields.entrySet()) {
      conf.set(kv.getKey().getName(), kv.getValue());
    }
    Job job = new Job(conf);
    job.setJarByClass(PcapJob.class);
    job.setMapperClass(PcapJob.PcapMapper.class);
    job.setMapOutputKeyClass(LongWritable.class);
    job.setMapOutputValueClass(BytesWritable.class);
    job.setReducerClass(PcapReducer.class);
    job.setOutputKeyClass(LongWritable.class);
    job.setOutputValueClass(BytesWritable.class);
    SequenceFileInputFormat.addInputPaths(job, Joiner.on(',').join(getPaths(fs, basePath, beginNS, endNS )));
    job.setInputFormatClass(SequenceFileInputFormat.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    SequenceFileOutputFormat.setOutputPath(job, outputPath);
    return job;

  }
}
