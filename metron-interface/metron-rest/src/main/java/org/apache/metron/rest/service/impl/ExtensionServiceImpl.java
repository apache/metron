/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.rest.service.impl;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.metron.guava.io.Files;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.ExtensionService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;

@Service
public class ExtensionServiceImpl implements ExtensionService{
  final static int BUFFER_SIZ = 2048;
  final static String[] CONFIG_EXT = {"json"};

  @Override
  public void install(ExtensionType extensionType, TarArchiveInputStream tgzStream) throws Exception{
     /*
      Operation need
      1. get a tar.gz stream for the file
      2. verify the structure and the contents
         a. must have a config dir
         b. MAY have a .bundle file
      3. send the configurations to the proper configuration services
      4. save the bundle to hdfs
     */
     // unpack
     Path unPackedPath = unpackExtension(tgzStream);

     if(extensionType == ExtensionType.PARSER){
       installParserExtension(unPackedPath);
     }
  }

  private void installParserExtension(Path extensionPath) throws Exception{
    // verify the structure
    verifyParserExtension(extensionPath);
  }

  private Path unpackExtension(TarArchiveInputStream tgzStream)throws Exception {
    File tmpDir = Files.createTempDir();
    TarArchiveEntry entry = null;
    while ((entry = (TarArchiveEntry) tgzStream.getNextEntry()) != null) {
      Path path = tmpDir.toPath();
      Path childPath = path.resolve(entry.getName());
      if (entry.isDirectory()) {
        childPath.toFile().mkdirs();
      } else {
        int count;
        byte data[] = new byte[BUFFER_SIZ];
        FileOutputStream fos = new FileOutputStream(childPath.toFile());
        BufferedOutputStream dest = new BufferedOutputStream(fos,
                BUFFER_SIZ);
        while ((count = tgzStream.read(data, 0, BUFFER_SIZ)) != -1) {
          dest.write(data, 0, count);
        }
        dest.close();
      }
    }
    return tmpDir.toPath();
  }

  private void verifyParserExtension(Path extensionPath) throws Exception{
    verifyParserExtensionConfiguration(extensionPath);
    verifyExtensionBundle(extensionPath);
  }

  private void verifyParserExtensionConfiguration(Path extensionPath) throws Exception{
    // parsers must have configurations
    // config/
    // config/zookeeper/
    // config/zookeeper/enrichments
    // config/zookeeper/indexing
    // config/zookeeper/parsers

    // there may be other, TBD

    Path config = extensionPath.resolve("config");
    if(!config.toFile().exists()){
      throw new RestException("Invalid Parser Extension: Missing configuration");
    }

    Path enrichments = config.resolve("zookeeper/enrichments");
    if((!enrichments.toFile().exists()) ||
      (FileUtils.listFiles(enrichments.toFile(),CONFIG_EXT,false).isEmpty())){
      throw new RestException("Invalid Parser Extension: Missing Enrichment Configuration ");
    }

    Path indexing = config.resolve("zookeeper/indexing");
    if((!indexing.toFile().exists()) ||
            (FileUtils.listFiles(indexing.toFile(),CONFIG_EXT,false).isEmpty())){
      throw new RestException("Invalid Parser Extension: Missing Indexing Configuration ");
    }

    Path parsers = config.resolve("zookeeper/parsers");
    if((!parsers.toFile().exists()) ||
            (FileUtils.listFiles(parsers.toFile(),CONFIG_EXT,false).isEmpty())){
      throw new RestException("Invalid Parser Extension: Missing Parsers Configuration ");
    }


  }

  private void verifyExtensionBundle(Path extensionPath) throws Exception{
    // check if there is a bundle at all
    // if there is verify that

  }
}
