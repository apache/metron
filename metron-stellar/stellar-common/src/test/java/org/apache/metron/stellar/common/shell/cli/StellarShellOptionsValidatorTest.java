/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.stellar.common.shell.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class StellarShellOptionsValidatorTest {

  @Test
  public void validateOptions() throws Exception {
    String[] validZHostArg = new String[]{"-z", "localhost:8888"};
    String[] validZHostArgNoPort = new String[]{"-z", "localhost"};
    String[] validZIPArgNoPort = new String[]{"-z", "10.10.10.3"};
    String[] validZHostArgList = new String[]{"-z", "localhost:8888,localhost:2181,localhost"};
    String[] validZIPArg = new String[]{"-z", "10.10.10.3:9999"};
    String[] invalidZNameArg = new String[]{"-z", "!!!@!!@!:8882"};
    String[] invalidZIPArg = new String[]{"-z", "11111.22222.10.3:3332"};
    String[] invalidZMissingNameArg = new String[]{"-z", ":8882"};
    String[] invalidZZeroPortArg = new String[]{"-z", "youtube.com:0"};
    String[] invalidZHugePortArg = new String[]{"-z", "youtube.com:75565"};


    String existingFileName = "./target/existsFile";
    String nonExistentFile = "./target/doesNotExist";

    String[] validVFileArg = new String[]{"-v", existingFileName};
    String[] validIrcFileArg = new String[]{"-irc", existingFileName};
    String[] validPFileArg = new String[]{"-p", existingFileName};
    String[] invalidVFileArg = new String[]{"-v", nonExistentFile};
    String[] invalidIrcFileArg = new String[]{"-irc", nonExistentFile};
    String[] invalidPFileArg = new String[]{"-p", nonExistentFile};

    File existingFile = new File(existingFileName);
    if (!existingFile.exists()) {
      existingFile.createNewFile();
    }
    Options options = new Options();
    options.addOption("z", "zookeeper", true, "Zookeeper URL");
    options.addOption("v", "variables", true, "File containing a JSON Map of variables");
    options.addOption("irc", "inputrc", true,
        "File containing the inputrc if not the default ~/.inputrc");
    options.addOption("na", "no_ansi", false, "Make the input prompt not use ANSI colors.");
    options.addOption("h", "help", false, "Print help");
    options.addOption("p", "properties", true, "File containing Stellar properties");

    CommandLineParser parser = new PosixParser();

    // these should work
    CommandLine commandLine = parser.parse(options, validZHostArg);
    StellarShellOptionsValidator.validateOptions(commandLine);

    commandLine = parser.parse(options, validZIPArg);
    StellarShellOptionsValidator.validateOptions(commandLine);

    commandLine = parser.parse(options, validVFileArg);
    StellarShellOptionsValidator.validateOptions(commandLine);

    commandLine = parser.parse(options, validIrcFileArg);
    StellarShellOptionsValidator.validateOptions(commandLine);

    commandLine = parser.parse(options, validPFileArg);
    StellarShellOptionsValidator.validateOptions(commandLine);

    commandLine = parser.parse(options, validZHostArgNoPort);
    StellarShellOptionsValidator.validateOptions(commandLine);

    commandLine = parser.parse(options, validZHostArgList);
    StellarShellOptionsValidator.validateOptions(commandLine);

    commandLine = parser.parse(options, validZIPArgNoPort);
    StellarShellOptionsValidator.validateOptions(commandLine);
    // these should not

    boolean thrown = false;


    try {
      commandLine = parser.parse(options, invalidZNameArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for providing invalid host name ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidZIPArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for providing invalid ip address ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidZMissingNameArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for only providing port ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidZZeroPortArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for 0 port ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidZHugePortArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for port out of range ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidVFileArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for passing non-existant file to -v ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidVFileArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for passing non-existant file to -v ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidIrcFileArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for passing non-existant file to -irc ", thrown);
    thrown = false;

    try {
      commandLine = parser.parse(options, invalidPFileArg);
      StellarShellOptionsValidator.validateOptions(commandLine);
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    Assert.assertTrue("Did not catch failure for passing non-existant file to -p ", thrown);
    thrown = false;
  }

}