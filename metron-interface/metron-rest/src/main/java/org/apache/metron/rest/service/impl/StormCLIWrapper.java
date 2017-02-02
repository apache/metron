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
package org.apache.metron.rest.service.impl;

import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.apache.metron.rest.MetronRestConstants.ENRICHMENT_TOPOLOGY_NAME;
import static org.apache.metron.rest.MetronRestConstants.INDEXING_TOPOLOGY_NAME;

public class StormCLIWrapper {

  private Environment environment;

  @Autowired
  public void setEnvironment(final Environment environment) {
    this.environment = environment;
  }

  public int startParserTopology(String name) throws RestException {
    return runCommand(getParserStartCommand(name));
  }

  public int stopParserTopology(String name, boolean stopNow) throws RestException {
    return runCommand(getStopCommand(name, stopNow));
  }

  public int startEnrichmentTopology() throws RestException {
    return runCommand(getEnrichmentStartCommand());
  }

  public int stopEnrichmentTopology(boolean stopNow) throws RestException {
    return runCommand(getStopCommand(ENRICHMENT_TOPOLOGY_NAME, stopNow));
  }

  public int startIndexingTopology() throws RestException {
    return runCommand(getIndexingStartCommand());
  }

  public int stopIndexingTopology(boolean stopNow) throws RestException {
    return runCommand(getStopCommand(INDEXING_TOPOLOGY_NAME, stopNow));
  }

  protected int runCommand(String[] command) throws RestException {
    ProcessBuilder pb = getProcessBuilder(command);
    pb.inheritIO();
    Process process = null;
    try {
      process = pb.start();
      process.waitFor();
    } catch (Exception e) {
      throw new RestException(e);
    }
    return process.exitValue();
  }

  protected String[] getParserStartCommand(String name) {
    String[] command = new String[7];
    command[0] = environment.getProperty(MetronRestConstants.PARSER_SCRIPT_PATH_SPRING_PROPERTY);
    command[1] = "-k";
    command[2] = environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY);
    command[3] = "-z";
    command[4] = environment.getProperty(MetronRestConstants.ZK_URL_SPRING_PROPERTY);
    command[5] = "-s";
    command[6] = name;
    return command;
  }

  protected String[] getEnrichmentStartCommand() {
    String[] command = new String[1];
    command[0] = environment.getProperty(MetronRestConstants.ENRICHMENT_SCRIPT_PATH_SPRING_PROPERTY);
    return command;
  }

  protected String[] getIndexingStartCommand() {
    String[] command = new String[1];
    command[0] = environment.getProperty(MetronRestConstants.INDEXING_SCRIPT_PATH_SPRING_PROPERTY);
    return command;
  }

  protected String[] getStopCommand(String name, boolean stopNow) {
    String[] command;
    if (stopNow) {
      command = new String[5];
      command[3] = "-w";
      command[4] = "0";
    } else {
      command = new String[3];
    }
    command[0] = "storm";
    command[1] = "kill";
    command[2] = name;
    return command;
  }

  protected ProcessBuilder getProcessBuilder(String... command) {
    return new ProcessBuilder(command);
  }

  public Map<String, String> getStormClientStatus() throws RestException {
    Map<String, String> status = new HashMap<>();
    status.put("parserScriptPath", environment.getProperty(MetronRestConstants.PARSER_SCRIPT_PATH_SPRING_PROPERTY));
    status.put("enrichmentScriptPath", environment.getProperty(MetronRestConstants.ENRICHMENT_SCRIPT_PATH_SPRING_PROPERTY));
    status.put("indexingScriptPath", environment.getProperty(MetronRestConstants.INDEXING_SCRIPT_PATH_SPRING_PROPERTY));
    status.put("stormClientVersionInstalled", stormClientVersionInstalled());
    return status;
  }

  protected String stormClientVersionInstalled() throws RestException {
    String stormClientVersionInstalled = "Storm client is not installed";
    ProcessBuilder pb = getProcessBuilder("storm", "version");
    pb.redirectErrorStream(true);
    Process p;
    try {
      p = pb.start();
    } catch (IOException e) {
      throw new RestException(e);
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    List<String> lines = reader.lines().collect(toList());
    lines.forEach(System.out::println);
    if (lines.size() > 1) {
      stormClientVersionInstalled = lines.get(1).replaceFirst("Storm ", "");
    }
    return stormClientVersionInstalled;
  }


}
