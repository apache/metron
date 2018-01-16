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
package org.apache.metron.dataloads.nonbulk.flatfile;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.nonbulk.flatfile.importer.LocalSummarizer;
import org.apache.metron.dataloads.nonbulk.flatfile.location.Location;
import org.apache.metron.dataloads.nonbulk.flatfile.location.RawLocation;
import org.apache.metron.dataloads.nonbulk.flatfile.writer.InvalidWriterOutput;
import org.apache.metron.dataloads.nonbulk.flatfile.writer.Writer;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class SimpleFlatFileSummarizerTest {
  /**
   {
   "config" : {
     "columns" : {
       "rank" : 0,
       "domain" : 1
     },
     "value_transform" : {
       "domain" : "DOMAIN_REMOVE_TLD(domain)"
     },
     "value_filter" : "LENGTH(domain) > 0",
     "state_init" : "MULTISET_INIT()",
     "state_update" : {
       "state" : "MULTISET_ADD(state, domain)"
     },
     "state_merge" : "MULTISET_MERGE(states)",
     "separator" : ","
     },
     "extractor" : "CSV"
   }
   */
  @Multiline
  public static String stellarExtractorConfigLineByLine;

  /**
   {
   "config" : {
     "columns" : {
       "rank" : 0,
       "domain" : 1
     },
     "value_transform" : {
       "domain" : "DOMAIN_REMOVE_TLD(domain)"
     },
     "value_filter" : "LENGTH(domain) > 0",
     "state_init" : "MULTISET_INIT()",
     "state_update" : {
       "state" : "MULTISET_ADD(state, domain)"
     },
     "state_merge" : "MULTISET_MERGE(states)",
     "separator" : ","
     },
     "extractor" : "CSV",
     "inputFormat" : "WHOLE_FILE"
   }
   */
  @Multiline
  public static String stellarExtractorConfigWholeFile;


  public static List<String> domains = ImmutableList.of(
          "google.com",
          "youtube.com",
          "facebook.com",
          "baidu.com",
          "wikipedia.org",
          "yahoo.com",
          "google.co.in",
          "reddit.com",
          "qq.com",
          "amazon.com",
          "taobao.com",
          "tmall.com",
          "twitter.com",
          "live.com",
          "vk.com",
          "google.co.jp",
          "instagram.com",
          "sohu.com",
          "sina.com.cn",
          "jd.com"
  );

  public static String generateData() {
    List<String> tmp = new ArrayList<>();
    int i = 1;
    for(String d : domains) {
      tmp.add(i + "," + d);
    }
    return Joiner.on("\n").join(tmp);
  }

  @Test
  public void testArgs() throws Exception {
    String[] argv = { "-e extractor.json"
            , "-o out.ser"
            , "-l log4j", "-i input.csv"
            , "-p 2", "-b 128", "-q"
    };

    Configuration config = new Configuration();
    String[] otherArgs = new GenericOptionsParser(config, argv).getRemainingArgs();

    CommandLine cli = SummarizeOptions.parse(new PosixParser(), otherArgs);
    Assert.assertEquals("extractor.json", SummarizeOptions.EXTRACTOR_CONFIG.get(cli).trim());
    Assert.assertEquals("input.csv", SummarizeOptions.INPUT.get(cli).trim());
    Assert.assertEquals("log4j", SummarizeOptions.LOG4J_PROPERTIES.get(cli).trim());
    Assert.assertEquals("2", SummarizeOptions.NUM_THREADS.get(cli).trim());
    Assert.assertEquals("128", SummarizeOptions.BATCH_SIZE.get(cli).trim());
  }

  public static class InMemoryLocation implements RawLocation {
    Map<String, String> inMemoryData;
    public InMemoryLocation(Map<String, String> inMemoryData)
    {
      this.inMemoryData = inMemoryData;
    }

    @Override
    public Optional<List<String>> list(String loc) throws IOException {
      if(loc.equals(".")) {
        ArrayList<String> ret = new ArrayList<>(inMemoryData.keySet());
        return Optional.of(ret);
      }
      return Optional.empty();
    }

    @Override
    public boolean exists(String loc) {
      return loc.equals(".") ? true:inMemoryData.containsKey(loc);
    }

    @Override
    public boolean isDirectory(String loc) throws IOException {
      return loc.equals(".")?true:false;
    }

    @Override
    public InputStream openInputStream(String loc) throws IOException {
      return new ByteArrayInputStream(inMemoryData.get(loc).getBytes());
    }

    @Override
    public boolean match(String loc) {
      return exists(loc);
    }
  }

  public class MockSummarizer extends LocalSummarizer {
    Map<String, String> mockData;
    public MockSummarizer(Map<String, String> mockData) {
      this.mockData = mockData;
    }

    @Override
    protected List<Location> getLocationsRecursive(List<String> inputs, FileSystem fs) throws IOException {
      Set<Location> ret = new HashSet<>();
      for(String input : inputs) {
        if(input.equals(".")) {
          for(String s : mockData.keySet()) {
            ret.add(resolveLocation(s, fs));
          }
        }
        else {
          ret.add(resolveLocation(input, fs));
        }
      }
      return new ArrayList<>(ret);
    }

    @Override
    protected Location resolveLocation(String input, FileSystem fs) {
      return new Location(input, new InMemoryLocation(mockData));
    }
  }

  public static class PeekingWriter implements Writer {
    AtomicReference<Object> ref;
    public PeekingWriter(AtomicReference<Object> ref) {
      this.ref = ref;
    }

    @Override
    public void validate(Optional<String> output, Configuration hadoopConfig) {

    }
    @Override
    public void write(Object obj, Optional<String> output, Configuration hadoopConfig) throws IOException {
      ref.set(obj);
    }

    @Override
    public void write(byte[] obj, Optional<String> output, Configuration hadoopConfig) throws IOException {

    }
  }

  @Test
  public void testLineByLine() throws IOException, InvalidWriterOutput {
    testLineByLine(5);
    testLineByLine(1);
  }

  public void testLineByLine(final int numThreads) throws IOException, InvalidWriterOutput {
    ExtractorHandler handler = ExtractorHandler.load(stellarExtractorConfigLineByLine);
    LocalSummarizer summarizer = new MockSummarizer(
            ImmutableMap.of("input.csv", generateData())
    );
    final AtomicReference<Object> finalObj = new AtomicReference<>(null);
    EnumMap<SummarizeOptions, Optional<Object>> options = new EnumMap<SummarizeOptions, Optional<Object>>(SummarizeOptions.class) {{
      put(SummarizeOptions.INPUT, Optional.of("input.csv"));
      put(SummarizeOptions.BATCH_SIZE, Optional.of(5));
      put(SummarizeOptions.QUIET, Optional.of(true));
      put(SummarizeOptions.OUTPUT_MODE, Optional.of(new PeekingWriter(finalObj)));
      put(SummarizeOptions.OUTPUT, Optional.of("out"));
      put(SummarizeOptions.NUM_THREADS, Optional.of(numThreads));
    }};
    summarizer.importData(options, handler, new Configuration());
    String expr = "MAP_GET(DOMAIN_REMOVE_TLD(domain), s) > 0";
    for(String domain : domains) {
      Boolean b = (Boolean)StellarProcessorUtils.run(expr, ImmutableMap.of("s", finalObj.get(), "domain", domain));
      Assert.assertTrue("Can't find " + domain, b);
    }
  }

  @Test
  public void testWholeFile() throws Exception {
    testWholeFile(5);
    testWholeFile(1);
  }

  public void testWholeFile(final int numThreads) throws IOException, InvalidWriterOutput {
    ExtractorHandler handler = ExtractorHandler.load(stellarExtractorConfigWholeFile);
    LocalSummarizer summarizer = new MockSummarizer(
            new HashMap<String, String>() {{
              for(String domain : domains) {
                put(domain, "1," + domain);
              }
            }}
    );
    final AtomicReference<Object> finalObj = new AtomicReference<>(null);
    EnumMap<SummarizeOptions, Optional<Object>> options = new EnumMap<SummarizeOptions, Optional<Object>>(SummarizeOptions.class) {{
      put(SummarizeOptions.INPUT, Optional.of("."));
      put(SummarizeOptions.BATCH_SIZE, Optional.of(5));
      put(SummarizeOptions.QUIET, Optional.of(true));
      put(SummarizeOptions.OUTPUT_MODE, Optional.of(new PeekingWriter(finalObj)));
      put(SummarizeOptions.OUTPUT, Optional.of("out"));
      put(SummarizeOptions.NUM_THREADS, Optional.of(numThreads));
    }};
    summarizer.importData(options, handler, new Configuration());
    String expr = "MAP_GET(DOMAIN_REMOVE_TLD(domain), s) > 0";
    for(String domain : domains) {
      Boolean b = (Boolean)StellarProcessorUtils.run(expr, ImmutableMap.of("s", finalObj.get(), "domain", domain));
      Assert.assertTrue("Can't find " + domain, b);
    }
  }

}
