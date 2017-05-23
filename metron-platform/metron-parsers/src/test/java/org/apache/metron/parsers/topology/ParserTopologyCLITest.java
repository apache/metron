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

package org.apache.metron.parsers.topology;

import org.apache.commons.cli.Parser;
import org.apache.log4j.Level;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.storm.Config;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ParserTopologyCLITest {


  public static class CLIBuilder {
    EnumMap<ParserTopologyCLI.ParserOptions, String> map = new EnumMap<>(ParserTopologyCLI.ParserOptions.class);

    public CLIBuilder with(ParserTopologyCLI.ParserOptions option, String val) {
      map.put(option, val);
      return this;
    }
    public CLIBuilder with(ParserTopologyCLI.ParserOptions option) {
      map.put(option, null);
      return this;
    }
    public CommandLine build(boolean longOpt) throws ParseException {
      return getCLI(map, longOpt);
    }
    private CommandLine getCLI(EnumMap<ParserTopologyCLI.ParserOptions, String> options, boolean longOpt) throws ParseException {
      ArrayList<String> args = new ArrayList<>();
      for (Map.Entry<ParserTopologyCLI.ParserOptions, String> option : options.entrySet()) {
        boolean hasLongOpt = option.getKey().option.hasLongOpt();
        if (hasLongOpt && longOpt) {
          args.add("--" + option.getKey().option.getLongOpt());
          if (option.getKey().option.hasArg() && option.getValue() != null) {
            args.add(option.getValue());
          }
        } else if (hasLongOpt && !longOpt) {
          args.add("-" + option.getKey().shortCode);
          if (option.getKey().option.hasArg() && option.getValue() != null) {
            args.add(option.getValue());
          }
        }
      }
      return ParserTopologyCLI.ParserOptions.parse(new PosixParser(), args.toArray(new String[args.size()]));
    }
  }

  @Test
  public void testNoOverlappingArgs() throws Exception {
    Set<String> optionStrs = new HashSet<>();
    for(ParserTopologyCLI.ParserOptions option : ParserTopologyCLI.ParserOptions.values()) {
      if(optionStrs.contains(option.option.getLongOpt())) {
        throw new IllegalStateException("Reused long option: " + option.option.getLongOpt());
      }
      if(optionStrs.contains(option.shortCode)) {
        throw new IllegalStateException("Reused short option: " + option.shortCode);
      }
      optionStrs.add(option.option.getLongOpt());
      optionStrs.add(option.shortCode);
    }
  }

  @Test
  public void testKafkaOffset_happyPath() throws ParseException {
    kafkaOffset(true);
    kafkaOffset(false);
  }
  public void kafkaOffset(boolean longOpt) throws ParseException {
    CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                      .build(longOpt);
    Assert.assertEquals("myzk", ParserTopologyCLI.ParserOptions.ZK_QUORUM.get(cli));
    Assert.assertEquals("mybroker", ParserTopologyCLI.ParserOptions.BROKER_URL.get(cli));
    Assert.assertEquals("mysensor", ParserTopologyCLI.ParserOptions.SENSOR_TYPE.get(cli));
  }
  @Test
  public void testCLI_happyPath() throws ParseException {
    happyPath(true);
    happyPath(false);
  }

  @Test(expected=ParseException.class)
  public void testCLI_insufficientArg() throws ParseException {
    UnitTestHelper.setLog4jLevel(Parser.class, Level.FATAL);
    CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .build(true);
    UnitTestHelper.setLog4jLevel(Parser.class, Level.ERROR);
  }

  public void happyPath(boolean longOpt) throws ParseException {
    CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                      .build(longOpt);
    Assert.assertEquals("myzk", ParserTopologyCLI.ParserOptions.ZK_QUORUM.get(cli));
    Assert.assertEquals("mybroker", ParserTopologyCLI.ParserOptions.BROKER_URL.get(cli));
    Assert.assertEquals("mysensor", ParserTopologyCLI.ParserOptions.SENSOR_TYPE.get(cli));
  }

  @Test
  public void testConfig_noExtra() throws ParseException {
    testConfig_noExtra(true);
    testConfig_noExtra(false);
  }

  public void testConfig_noExtra(boolean longOpt) throws ParseException {
   CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                     .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                     .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_WORKERS, "1")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_ACKERS, "2")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_MAX_TASK_PARALLELISM, "3")
                                     .with(ParserTopologyCLI.ParserOptions.MESSAGE_TIMEOUT, "4")
                                     .build(longOpt);
    Config config = ParserTopologyCLI.ParserOptions.getConfig(cli);
    Assert.assertEquals(1, config.get(Config.TOPOLOGY_WORKERS));
    Assert.assertEquals(2, config.get(Config.TOPOLOGY_ACKER_EXECUTORS));
    Assert.assertEquals(3, config.get(Config.TOPOLOGY_MAX_TASK_PARALLELISM));
    Assert.assertEquals(4, config.get(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS));
  }

  @Test
  public void testOutputTopic() throws Exception {
    testOutputTopic(true);
    testOutputTopic(false);
  }

  public void testOutputTopic(boolean longOpt) throws ParseException {
     CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                      .with(ParserTopologyCLI.ParserOptions.OUTPUT_TOPIC, "my_topic")
                                      .build(longOpt);
    Assert.assertEquals("my_topic", ParserTopologyCLI.ParserOptions.OUTPUT_TOPIC.get(cli));
  }

  /**
    {
      "string" : "foo"
     ,"integer" : 1
    }
   */
  @Multiline
  public static String extraConfig;

  @Test
  public void testConfig_extra() throws Exception {
    testConfig_extra(true);
    testConfig_extra(false);
  }

  public void testConfig_extra(boolean longOpt) throws IOException, ParseException {
    File extraFile = File.createTempFile("extra", "json");
    try {
      FileUtils.write(extraFile, extraConfig);
      CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
              .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
              .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
              .with(ParserTopologyCLI.ParserOptions.MESSAGE_TIMEOUT, "4")
              .with(ParserTopologyCLI.ParserOptions.EXTRA_OPTIONS, extraFile.getAbsolutePath())
              .build(longOpt);
      Config config = ParserTopologyCLI.ParserOptions.getConfig(cli);
      Assert.assertEquals(4, config.get(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS));
      Assert.assertEquals("foo", config.get("string"));
      Assert.assertEquals(1, config.get("integer"));
    } finally{
      extraFile.deleteOnExit();
    }
  }
}
