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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DockerUtils {

  public static String getDockerIpAddress() {
    String ipAddress = "localhost";
    ProcessBuilder pb = new ProcessBuilder("docker-machine", "ip", "metron-machine");
    final Process process;
    try {
      process = pb.start();
      process.waitFor();
      if (process.exitValue() == 0) {
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
        ipAddress = inputStream.readLine();
      }
    } catch (IOException | InterruptedException e) {
      // Assume ip is localhost
    }
    return ipAddress;
  }

  public static void main(String[] args) {
    System.out.println(DockerUtils.getDockerIpAddress());
  }
}
