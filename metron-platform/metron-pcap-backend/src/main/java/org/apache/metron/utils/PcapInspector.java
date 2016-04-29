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

package org.apache.metron.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.common.Constants;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

public class PcapInspector {
  private static abstract class OptionHandler implements Function<String, Option> {}
  private enum InspectorOptions {
    HELP("h", new OptionHandler() {

      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        return new Option(s, "help", false, "Generate Help screen");
      }
    })
    ,INPUT("i", new OptionHandler() {

      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "input", true, "Input sequence file on HDFS");
        o.setArgName("SEQ_FILE");
        o.setRequired(true);
        return o;
      }
    })
    ,NUM("n", new OptionHandler() {

      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "num_packets", true, "Number of packets to dump");
        o.setArgName("N");
        o.setRequired(false);
        return o;
      }
    })
    ;
    Option option;
    String shortCode;

    InspectorOptions(String shortCode, OptionHandler optionHandler) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }

    public static CommandLine parse(CommandLineParser parser, String[] args) {
      try {
        CommandLine cli = parser.parse(getOptions(), args);
        if (InspectorOptions.HELP.has(cli)) {
          printHelp();
          System.exit(0);
        }
        return cli;
      } catch (ParseException e) {
        System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
        e.printStackTrace(System.err);
        printHelp();
        System.exit(-1);
        return null;
      }
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("PcapInspector", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for (InspectorOptions o : InspectorOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  public static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance( SimpleDateFormat.LONG
                                                                                   , SimpleDateFormat.LONG
                                                                                   );
  public static void main(String... argv) throws IOException {
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();
    CommandLine cli = InspectorOptions.parse(new PosixParser(), otherArgs);
    Path inputPath = new Path(InspectorOptions.INPUT.get(cli));
    int n = -1;
    if(InspectorOptions.NUM.has(cli)) {
      n = Integer.parseInt(InspectorOptions.NUM.get(cli));
    }
    SequenceFile.Reader reader = new SequenceFile.Reader(new Configuration(),
            SequenceFile.Reader.file(inputPath)
    );
    LongWritable key = new LongWritable();
    BytesWritable value = new BytesWritable();

    for(int i = 0;(n < 0 || i < n) && reader.next(key, value);++i) {
      long millis = Long.divideUnsigned(key.get(), 1000000);
      String ts = DATE_FORMAT.format(new Date(millis));
      for(PacketInfo pi : PcapHelper.toPacketInfo(value.copyBytes())) {
        EnumMap<Constants.Fields, Object> result = PcapHelper.packetToFields(pi);
        List<String> fieldResults = new ArrayList<String>() {{
          add("TS: " + ts);
        }};
        for(Constants.Fields field : Constants.Fields.values()) {
          if(result.containsKey(field)) {
            fieldResults.add(field + ": " + result.get(field));
          }
        }
        System.out.println(Joiner.on(",").join(fieldResults));
      }
    }
  }
}
