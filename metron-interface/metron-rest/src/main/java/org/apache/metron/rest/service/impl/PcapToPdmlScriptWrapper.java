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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.pcap.Pdml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PcapToPdmlScriptWrapper {

  public InputStream execute(String scriptPath, FileSystem fileSystem, Path pcapPath) throws IOException {
    ProcessBuilder processBuilder = getProcessBuilder(scriptPath, pcapPath.toUri().getPath());
    Process process = processBuilder.start();
    InputStream rawInputStream = getRawInputStream(fileSystem, pcapPath);
    OutputStream processOutputStream = process.getOutputStream();
    IOUtils.copy(rawInputStream, processOutputStream);
    rawInputStream.close();
    if (process.isAlive()) {
      // need to close processOutputStream if script doesn't exit with an error
      processOutputStream.close();
      return process.getInputStream();
    } else {
      String errorMessage = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
      throw new IOException(errorMessage);
    }
  }

  protected InputStream getRawInputStream(FileSystem fileSystem, Path path) throws IOException {
    return fileSystem.open(path);
  }

  protected ProcessBuilder getProcessBuilder(String... command) {
    return new ProcessBuilder(command);
  }
}
