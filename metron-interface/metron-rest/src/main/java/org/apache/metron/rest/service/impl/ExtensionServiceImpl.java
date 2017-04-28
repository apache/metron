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
import org.apache.metron.rest.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExtensionServiceImpl implements ExtensionService{
  final static int BUFFER_SIZ = 2048;
  final static String[] CONFIG_EXT = {"json"};
  final static String[] BUNDLE_EXT = {"bundle"};

  @Autowired
  HdfsService hdfsService;
  @Autowired
  SensorEnrichmentConfigService sensorEnrichmentConfigService;
  @Autowired
  SensorIndexingConfigService sensorIndexingConfigService;
  @Autowired
  SensorParserConfigService sensorParserConfigService;

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
    Map<Paths,Path> context = new HashMap<>();
    // verify the structure
    verifyParserExtension(extensionPath, context);

    // after verification we will have all the paths we need
    // TODO - we want to keep a list of things we have pushed to zk, so if one fails we can remove them and 'roll back'

    // LOAD each config from bytes ( from path ( from file ) )
    // SAVE Using Appropriate service

    // Load bundle to bytes, save to hdfs
  }

  private Path unpackExtension(TarArchiveInputStream tgzStream)throws Exception {
    File tmpDir = Files.createTempDir();
    tmpDir.deleteOnExit();
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
      childPath.toFile().deleteOnExit();
    }
    return tmpDir.toPath();
  }

  private void verifyParserExtension(Path extensionPath, Map<Paths,Path> context) throws Exception{
    verifyParserExtensionConfiguration(extensionPath, context);
    verifyExtensionBundle(extensionPath,context);
  }

  private void verifyParserExtensionConfiguration(Path extensionPath, Map<Paths,Path> context) throws Exception{
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
    context.put(Paths.CONFIG,config);

    Path enrichments = config.resolve("zookeeper/enrichments");
    if(!enrichments.toFile().exists()){
      throw new RestException("Invalid Parser Extension: Missing Enrichment Configuration ");
    }
    Collection<File> configurations = FileUtils.listFiles(enrichments.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new RestException("Invalid Parser Extension: Missing Enrichment Configuration ");
    }
    context.put(Paths.ENRICHMENTS_CONFIG,configurations.stream().findFirst().get().toPath());
    context.put(Paths.ZOOKEEPER,enrichments.getParent());
    context.put(Paths.ENRICHMENTS_CONFIG_DIR,enrichments);

    Path indexing = config.resolve("zookeeper/indexing");
    if(!indexing.toFile().exists()){
      throw new RestException("Invalid Parser Extension: Missing Indexing Configuration ");
    }
    configurations = FileUtils.listFiles(indexing.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new RestException("Invalid Parser Extension: Missing Indexing Configuration ");
    }
    context.put(Paths.INDEXING_CONFIG,configurations.stream().findFirst().get().toPath());
    context.put(Paths.INDEXING_CONFIG_DIR,indexing);

    Path parsers = config.resolve("zookeeper/parsers");
    if(!parsers.toFile().exists()){
      throw new RestException("Invalid Parser Extension: Missing Parsers Configuration ");
    }
    configurations = FileUtils.listFiles(parsers.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new RestException("Invalid Parser Extension: Missing Parsers Configuration ");
    }
    context.put(Paths.PARSERS_CONFIG,configurations.stream().findFirst().get().toPath());
    context.put(Paths.PARSERS_CONFIG_DIR,parsers);

  }

  private void verifyExtensionBundle(Path extensionPath, Map<Paths,Path> context) throws Exception{
    // check if there is a bundle at all
    // if there is verify that
    Collection<File> bundles = FileUtils.listFiles(extensionPath.toFile(),BUNDLE_EXT,false);
    if(bundles.isEmpty()){
      // this is a configuration only parser
      // which is ok
      return;
    }
    context.put(Paths.BUNDLE,bundles.stream().findFirst().get().toPath());

    // TODO - load the bundle and verify the metadata
    // TODO - get the bundle.properties out of zk to get the correct prefixes etc
  }
}
