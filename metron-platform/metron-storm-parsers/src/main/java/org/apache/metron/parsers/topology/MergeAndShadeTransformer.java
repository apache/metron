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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.apache.storm.daemon.JarTransformer;
import org.apache.storm.hack.StormShadeTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a storm jar transformer that will add in additional jars pulled from an
 * environment variable.  The jars will be merged with the main uber jar and then
 * the resulting jar will be shaded and relocated according to the StormShadeTransformer.
 *
 */
public class MergeAndShadeTransformer implements JarTransformer {
  public static final String EXTRA_JARS_ENV = "EXTRA_JARS";
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private StormShadeTransformer underlyingTransformer = new StormShadeTransformer();
  @Override
  public void transform(InputStream input, OutputStream output) throws IOException {
    String extraJars = System.getenv().get(EXTRA_JARS_ENV);
    if(extraJars == null || extraJars.length() == 0) {
      underlyingTransformer.transform(input, output);
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
        LOG.info("Merging jar {} from {}", f.getName(), f.getAbsolutePath());
        try (JarInputStream jin = new JarInputStream(new BufferedInputStream(new FileInputStream(f)))) {
          copy(jin, jout, entries);
        }
      }
    }
    underlyingTransformer.transform(new BufferedInputStream(new FileInputStream(tmpFile)), output);
  }

  /**
   * Merges two jars.  The first jar will get merged into the output jar.
   * A running set of jar entries is kept so that duplicates are skipped.
   * This has the side-effect that the first instance of a given entry will be added
   * and all subsequent entries are skipped.
   *
   * @param jin The input jar
   * @param jout The output jar
   * @param entries The set of existing entries.  Note that this set will be mutated as part of this call.
   * @return The set of entries.
   * @throws IOException
   */
  private Set<String> copy(JarInputStream jin, JarOutputStream jout, Set<String> entries) throws IOException {
    byte[] buffer = new byte[1024];
    for(JarEntry entry = jin.getNextJarEntry(); entry != null; entry = jin.getNextJarEntry()) {
      if(entries.contains(entry.getName())) {
        continue;
      }
      LOG.debug("Merging jar entry {}", entry.getName());
      entries.add(entry.getName());
      jout.putNextEntry(entry);
      int len = 0;
      while( (len = jin.read(buffer)) > 0 ) {
        jout.write(buffer, 0, len);
      }
    }
    return entries;
  }
}
