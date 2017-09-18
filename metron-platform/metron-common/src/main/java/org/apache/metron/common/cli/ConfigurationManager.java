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

package org.apache.metron.common.cli;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationManager {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public enum ConfigurationOptions {
    HELP("h", s -> new Option(s, "help", false, "Generate Help screen")),
    INPUT("i", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("input_dir")
        .withArgName("DIR")
        .withDescription("The input directory containing the configuration files named like \"$source.json\"")
        .create(s)
    ),
    OUTPUT("o", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("output_dir")
        .withArgName("DIR")
        .withDescription("The output directory which will store the JSON configuration from Zookeeper")
        .create(s)
    ),
    ZK_QUORUM("z", s -> OptionBuilder.isRequired(true)
        .hasArg()
        .withLongOpt("zk_quorum")
        .withArgName("host:port,[host:port]*")
        .withDescription("Zookeeper Quorum URL (zk1:port,zk2:port,...)")
        .create(s)
    ),
    MODE("m", s -> OptionBuilder.isRequired(true)
        .hasArg()
        .withLongOpt("mode")
        .withArgName("MODE")
        .withDescription("The mode of operation: DUMP, PULL, PUSH, PATCH")
        .create(s)
    ),
    CONFIG_TYPE("c", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("config_type")
        .withArgName("CONFIG_TYPE")
        .withDescription("The configuration type: GLOBAL, PARSER, ENRICHMENT, INDEXING, PROFILER")
        .create(s)
        ),
    CONFIG_NAME("n", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("config_name")
        .withArgName("CONFIG_NAME")
        .withDescription("The configuration name: bro, yaf, snort, squid, etc.")
        .create(s)
        ),
    PATCH_FILE("pf", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("patch_file")
        .withArgName("PATCH_FILE")
        .withDescription("Path to the patch file.")
        .create(s)
    ),
    PATCH_MODE("pm", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("patch_mode")
        .withArgName("PATCH_MODE")
        .withDescription("One of: ADD, REMOVE - relevant only for key/value patches, i.e. when a patch file is not used.")
        .create(s)
    ),
    PATCH_KEY("pk", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("patch_key")
        .withArgName("PATCH_KEY")
        .withDescription("The key to modify")
        .create(s)
    ),
    PATCH_VALUE("pv", s -> OptionBuilder.isRequired(false)
        .hasArg()
        .withLongOpt("patch_value")
        .withArgName("PATCH_VALUE")
        .withDescription("Value to use in the patch.")
        .create(s)
    ),
    FORCE("f", s -> new Option(s, "force", false, "Force operation"))
    ;
    Option option;
    String shortCode;
    ConfigurationOptions(String shortCode, Function<String, Option> optionHandler) {
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
        if(ConfigurationOptions.HELP.has(cli)) {
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
      formatter.printHelp( "configuration_manager", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(ConfigurationOptions o : ConfigurationOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  public enum PatchMode {
    ADD, REMOVE, REPLACE, MOVE, COPY, TEST;
  }

  /**
   * Dumps all config
   * @param client
   * @throws Exception
   */
  public void dump(CuratorFramework client) throws Exception {
    ConfigurationsUtils.dumpConfigs(System.out, client);
  }

  /**
   * Dumps specific config type only
   * @param client
   * @param type
   * @throws Exception
   */
  public void dump(CuratorFramework client, ConfigurationType type, Optional<String> configName) throws Exception {
    ConfigurationsUtils.dumpConfigs(System.out, client, type, configName);
  }

  public void pull(CuratorFramework client, String outFileStr, final boolean force) throws Exception {
    final File outputDir = new File(outFileStr);
    if (!outputDir.exists()) {
      if (!outputDir.mkdirs()) {
        throw new IllegalStateException("Unable to make directories: " + outputDir.getAbsolutePath());
      }
    }

    ConfigurationsUtils.visitConfigs(client, new ConfigurationsUtils.ConfigurationVisitor() {
      @Override
      public void visit(ConfigurationType configurationType, String name, String data) {
        File out = getFile(outputDir, configurationType, name);
        if (!out.exists() || force) {
          if (!out.exists()) {
            out.getParentFile().mkdirs();
          }
          try {
            Files.write(data, out, Charset.defaultCharset());
          } catch (IOException e) {
            throw new RuntimeException("Sorry, something bad happened writing the config to " + out.getAbsolutePath() + ": " + e.getMessage(), e);
          }
        } else if (out.exists() && !force) {
          throw new IllegalStateException("Unable to overwrite existing file (" + out.getAbsolutePath() + ") without the force flag (-f or --force) being set.");
        }
      }
    });
  }

  public void push(String inputDirStr, CuratorFramework client) throws Exception {
    final File inputDir = new File(inputDirStr);

    if (!inputDir.exists() || !inputDir.isDirectory()) {
      throw new IllegalStateException("Input directory: " + inputDir + " does not exist or is not a directory.");
    }
    ConfigurationsUtils.uploadConfigsToZookeeper(inputDirStr, client);
  }

  public void push(String inputDirStr, CuratorFramework client, ConfigurationType type, Optional<String> configName)
      throws Exception {
    final File inputDir = new File(inputDirStr);

    if (!inputDir.exists() || !inputDir.isDirectory()) {
      throw new IllegalStateException(
          "Input directory: " + inputDir + " does not exist or is not a directory.");
    }
    ConfigurationsUtils.uploadConfigsToZookeeper(inputDirStr, client, type, configName);
  }

  public void run(CommandLine cli) throws Exception {
    try(CuratorFramework client = ConfigurationsUtils.getClient(ConfigurationOptions.ZK_QUORUM.get(cli))) {
      client.start();
      run(client, cli);
    }
  }
  public void run(CuratorFramework client, CommandLine cli) throws Exception {
    final boolean force = ConfigurationOptions.FORCE.has(cli);
    String mode = ConfigurationOptions.MODE.get(cli);
    Optional<String> configType = Optional.ofNullable(ConfigurationOptions.CONFIG_TYPE.get(cli));
    Optional<String> configName = Optional.ofNullable(ConfigurationOptions.CONFIG_NAME.get(cli));

    switch (mode.toLowerCase()) {

      case "push":
        String inputDirStr = ConfigurationOptions.INPUT.get(cli);
        if (StringUtils.isEmpty(inputDirStr)) {
          throw new IllegalArgumentException("Input directory is required when performing a PUSH operation.");
        }
        if (configType.isPresent()) {
          push(inputDirStr, client, ConfigurationType.valueOf(configType.get()), configName);
        } else {
          push(inputDirStr, client);
        }

      case "dump":
        if (configType.isPresent()) {
          dump(client, ConfigurationType.valueOf(configType.get()), configName);
        } else {
          dump(client);
        }
        break;

      case "pull":
        pull(client, ConfigurationOptions.OUTPUT.get(cli), force);
        break;

      case "patch":
        if(configType.isPresent()) {
          Optional<String> patchPath = Optional.ofNullable(ConfigurationOptions.PATCH_FILE.get(cli));
          Optional<String> patchMode = Optional.ofNullable(ConfigurationOptions.PATCH_MODE.get(cli));
          Optional<String> patchKey = Optional.ofNullable(ConfigurationOptions.PATCH_KEY.get(cli));
          Optional<String> patchValue = Optional.ofNullable(ConfigurationOptions.PATCH_VALUE.get(cli));
          patch(client, ConfigurationType.valueOf(configType.get()), configName, patchMode, patchPath, patchKey, patchValue);
        } else {
          throw new IllegalArgumentException("Patch requires config type");
        }
        break;

      default:
        throw new IllegalStateException("Invalid mode: " + mode + " expected DUMP, PULL, PUSH, or PATCH");
    }

  }

  private void patch(CuratorFramework client, ConfigurationType configType,
      Optional<String> configName, Optional<String> patchMode, Optional<String> patchPath,
      Optional<String> patchKey, Optional<String> patchValue) {
    try {
      byte[] patchData =  null;
      if (patchKey.isPresent()) {
        patchData = buildPatch(patchMode, patchKey, patchValue).getBytes(StandardCharsets.UTF_8);
      } else {
        patchData = java.nio.file.Files.readAllBytes(Paths.get(patchPath.get()));
      }
      ConfigurationsUtils.applyConfigPatchToZookeeper(configType, configName, patchData, client);
    } catch (IOException e) {
      LOG.error("Unable to load patch file '%s'", patchPath, e);
    } catch (Exception e) {
      LOG.error("Unable to apply patch to Zookeeper config", e);
    }
  }

  private String buildPatch(Optional<String> patchMode, Optional<String> patchKey,
      Optional<String> patchValue) {
    PatchMode mode = PatchMode.ADD;
    if (patchMode.isPresent()) {
      mode = PatchMode.valueOf(patchMode.get());
    }
    String patch = "";
    switch (mode) {
      case ADD:
        if (!patchKey.isPresent() || !patchValue.isPresent()) {
          throw new IllegalArgumentException(
              "Key and value are required to apply patches without a file");
        }
        patch = String.format("[ { \"op\": \"%s\", \"path\": \"%s\", \"value\": %s } ]",
            patchMode.get().toString().toLowerCase(),
            patchKey.get(),
            patchValue.get());
        break;
      case REMOVE:
        if (!patchKey.isPresent()) {
          throw new IllegalArgumentException(
              "Key is required to apply a remove patch without a file");
        }
        patch = String.format("[ { \"op\": \"%s\", \"path\": \"%s\" } ]",
            patchMode.get().toString().toLowerCase(),
            patchKey.get());
        break;
      default:
        throw new UnsupportedOperationException("Patch mode not supported: " + mode.toString());
    }
    return patch;
  }

  private static File getFile(File baseDir, ConfigurationType configurationType, String name) {
    return new File(new File(baseDir, configurationType.getDirectory()), name + ".json");
  }

  public static void main(String... argv) throws Exception {
    CommandLineParser parser = new PosixParser();
    CommandLine cli = ConfigurationOptions.parse(parser, argv);
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(cli);
  }
}
