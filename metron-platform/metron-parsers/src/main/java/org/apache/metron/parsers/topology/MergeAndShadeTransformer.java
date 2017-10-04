/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers.topology;

import com.google.common.base.Splitter;
import org.apache.storm.daemon.JarTransformer;
import org.apache.storm.hack.StormShadeTransformer;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class MergeAndShadeTransformer implements JarTransformer {
  public static final String EXTRA_JARS_ENV = "EXTRA_JARS";
  StormShadeTransformer _underlyingTransformer = new StormShadeTransformer();
  @Override
  public void transform(InputStream input, OutputStream output) throws IOException {
    String extraJars = System.getenv().get(EXTRA_JARS_ENV);
    if(extraJars == null || extraJars.length() == 0) {
      _underlyingTransformer.transform(input, output);
      return;
    }
    File tmpFile = File.createTempFile("metron", "jar");
    tmpFile.deleteOnExit();
    Set<String> entries = new HashSet<>();
    try (JarOutputStream jout = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
      try (JarInputStream jin = new JarInputStream(new BufferedInputStream(input))){
        copy(jin, jout, entries);
      }
      for (String fileStr : Splitter.on(",").split(extraJars)) {
        File f = new File(fileStr);
        if (!f.exists()) {
          continue;
        }
        System.out.println("Merging " + f.getName());
        try (JarInputStream jin = new JarInputStream(new BufferedInputStream(new FileInputStream(f)))) {
          copy(jin, jout, entries);
        }
      }
    }
    _underlyingTransformer.transform(new BufferedInputStream(new FileInputStream(tmpFile)), output);
  }

  private void copy(JarInputStream jin, JarOutputStream jout, Set<String> entries) throws IOException {
    byte[] buffer = new byte[1024];
    for(JarEntry entry = jin.getNextJarEntry(); entry != null; entry = jin.getNextJarEntry()) {
      if(entries.contains(entry.getName())) {
        continue;
      }
      entries.add(entry.getName());
      jout.putNextEntry(entry);
      int len = 0;
      while( (len = jin.read(buffer)) > 0 ) {
        jout.write(buffer, 0, len);
      }
    }
  }
}
