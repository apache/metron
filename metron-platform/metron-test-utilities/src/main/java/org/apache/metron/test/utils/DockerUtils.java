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
package org.apache.metron.test.utils;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used by integration tests to determine how to connect to Docker containers.
 */
public class DockerUtils {

  public static String DEFAULT_DOCKER_HOST = "localhost";

  /**
   * Returns the Docker ip address that should be used to connect to containers.  It is assumed Docker engine
   * is running on localhost unless docker-machine is installed, a machine called "metron-machine" exists, and a proper
   * ip address for that machine is found.
   * @return
   */
  public static String getDockerIpAddress() {
    String ipAddress = DEFAULT_DOCKER_HOST;
    ProcessBuilder pb = new ProcessBuilder("docker-machine", "ip", "metron-machine");
    final Process process;
    try {
      process = pb.start();
      process.waitFor();
      // Assume localhost if exit code is not 0
      if (process.exitValue() == 0) {
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String response = inputStream.readLine();
        // Assume localhost if response is not a proper ip address
        if (InetAddressUtils.isIPv4Address(response) || InetAddressUtils.isIPv6Address(response)) {
          ipAddress = response;
        }
      }
    } catch (IOException | InterruptedException e) {
      // Assume localhost if an exception is thrown
    }
    return ipAddress;
  }

  /**
   * Returns the mapped port for a container.  If the original port is not mapped for this container, it is simply returned back.
   * @param dockerIpAddress
   * @param containerName
   * @param originalPort
   * @return
   */
  public static String getContainerPort(String dockerIpAddress, String containerName, String originalPort) {
    String port = originalPort;
    ProcessBuilder pb = new ProcessBuilder("docker", "port", containerName, originalPort);
    if (!DEFAULT_DOCKER_HOST.equals(dockerIpAddress)) {
      pb.environment().putAll(DockerUtils.getDockerEnv());
    }
    final Process process;
    try {
      process = pb.start();
      process.waitFor();
      // Assume expected port if exit code is not 0
      if (process.exitValue() == 0) {
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String response = inputStream.readLine();

        // Assume expected port if response is not formatted correctly
        if (response.startsWith("0.0.0.0")) {
          port = response.replaceFirst("0.0.0.0:", "");
        }
      }
    } catch (IOException | InterruptedException e) {
      // Assume expected port if an exception is thrown
      e.printStackTrace();
    }
    return port;
  }

  /**
   * Returns the Docker environment variables needed to configure a process to connect to metron-machine
   * @return
   */
  private static Map<String, String> getDockerEnv() {
    Map<String, String> env = new HashMap<>();
    ProcessBuilder pb = new ProcessBuilder("docker-machine", "env", "metron-machine");
    final Process process;
    try {
      process = pb.start();
      process.waitFor();
      // Assume empty environment if exit code is not 0
      if (process.exitValue() == 0) {
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String response;
        while((response = inputStream.readLine()) != null) {
          if (response.startsWith("export")) {
            String[] kv = response.replaceFirst("export ", "").split("=");
            env.put(kv[0], kv[1].replaceAll("\"", ""));
          }
        }
      }
    } catch (IOException | InterruptedException e) {
      // Assume empty environment if an exception is thrown
    }
    return env;
  }

}
