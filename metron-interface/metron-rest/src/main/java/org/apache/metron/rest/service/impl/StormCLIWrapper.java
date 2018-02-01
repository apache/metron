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

import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.apache.metron.rest.MetronRestConstants.ENRICHMENT_TOPOLOGY_NAME;
import static org.apache.metron.rest.MetronRestConstants.INDEXING_TOPOLOGY_NAME;

public class StormCLIWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Environment environment;

  @Autowired
  public void setEnvironment(final Environment environment) {
    this.environment = environment;
  }

  public int startParserTopology(String name) throws RestException {
    kinit();
    return runCommand(getParserStartCommand(name));
  }

  public int stopParserTopology(String name, boolean stopNow) throws RestException {
    kinit();
    return runCommand(getStopCommand(name, stopNow));
  }

  public int startEnrichmentTopology() throws RestException {
    kinit();
    return runCommand(getEnrichmentStartCommand());
  }

  public int stopEnrichmentTopology(boolean stopNow) throws RestException {
    kinit();
    return runCommand(getStopCommand(ENRICHMENT_TOPOLOGY_NAME, stopNow));
  }

  public int startIndexingTopology() throws RestException {
    kinit();
    return runCommand(getIndexingStartCommand());
  }

  public int stopIndexingTopology(boolean stopNow) throws RestException {
    kinit();
    return runCommand(getStopCommand(INDEXING_TOPOLOGY_NAME, stopNow));
  }

  protected int runCommand(String[] command) throws RestException {
    ProcessBuilder pb = getProcessBuilder(command);
    pb.inheritIO();
    LOG.debug("Running command: cmd={}", String.join(" ", command));

    Process process;
    try {
      process = pb.start();
      process.waitFor();

    } catch (Exception e) {
      throw new RestException(e);
    }

    int exitValue = process.exitValue();
    LOG.debug("Command completed: cmd={}, exit={}", String.join(" ", command), exitValue);

    return exitValue;
  }

  protected String[] getParserStartCommand(String name) {
    List<String> command = new ArrayList<>();
    command.add( environment.getProperty(MetronRestConstants.PARSER_SCRIPT_PATH_SPRING_PROPERTY));

    // sensor type
    command.add( "-s");
    command.add( name);

    // zookeeper
    command.add( "-z");
    command.add( environment.getProperty(MetronRestConstants.ZK_URL_SPRING_PROPERTY));

    // kafka broker
    command.add( "-k");
    command.add( environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY));

    // kafka security protocol
    command.add( "-ksp");
    command.add(KafkaUtils.INSTANCE.normalizeProtocol(environment.getProperty(MetronRestConstants.KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY)));

    // extra topology options
    boolean kerberosEnabled = environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false);
    boolean topologyOptionsDefined = StringUtils.isNotBlank(environment.getProperty(MetronRestConstants.PARSER_TOPOLOGY_OPTIONS_SPRING_PROPERTY));
    if (kerberosEnabled && topologyOptionsDefined) {
        command.add("-e");
        command.add(environment.getProperty(MetronRestConstants.PARSER_TOPOLOGY_OPTIONS_SPRING_PROPERTY));
    }

    return command.toArray(new String[0]);
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

  protected void kinit() throws RestException {
    if (environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)) {
      String keyTabLocation = environment.getProperty(MetronRestConstants.KERBEROS_KEYTAB_SPRING_PROPERTY);
      String userPrincipal = environment.getProperty(MetronRestConstants.KERBEROS_PRINCIPLE_SPRING_PROPERTY);
      String[] kinitCommand = {"kinit", "-kt", keyTabLocation, userPrincipal};
      runCommand(kinitCommand);
    }
  }


}
