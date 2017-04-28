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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.guava.io.Files;
import org.apache.metron.rest.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

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

  private CuratorFramework client;

  @Autowired
  public ExtensionServiceImpl(CuratorFramework client) {
    this.client = client;
  }

  @Override
  public void install(ExtensionType extensionType, TarArchiveInputStream tgzStream) throws Exception{
     // unpack
     Path unPackedPath = unpackExtension(tgzStream);

     if(extensionType == ExtensionType.PARSER){
       installParserExtension(unPackedPath);
     }
     // stellar etc....
  }

  private void installParserExtension(Path extensionPath) throws Exception{
    final Map<Paths,List<Path>> context = new HashMap<>();
    // verify the structure
    verifyParserExtension(extensionPath, context);

    // after verification we will have all the paths we need
    // we want to keep a list of things we have pushed to zk, so if one fails we can remove them and 'roll back'
    // we only need to pass the name to the delete
    // we do the hdfs deploy last, so we should be able to avoid deleting it after lay down
    final List<String> loadedEnrichementConfigs = new ArrayList<>();
    final List<String> loadedIndexingConfigs = new ArrayList<>();
    final List<String> loadedParserConfigs = new ArrayList<>();

    try{
        saveEnrichmentConfigs(context, loadedEnrichementConfigs);
        saveIndexingConfigs(context, loadedIndexingConfigs);
        saveParserConfigs(context, loadedParserConfigs);
        deployBundleToHdfs(context);
    }catch(Exception e){
      for(String name : loadedEnrichementConfigs){
        sensorEnrichmentConfigService.delete(name);
      }
      for(String name : loadedIndexingConfigs){
        sensorIndexingConfigService.delete(name);
      }
      for(String name : loadedParserConfigs){
        sensorParserConfigService.delete(name);
      }
      throw e;
    }
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

  private void verifyParserExtension(Path extensionPath, Map<Paths,List<Path>> context) throws Exception{
    verifyParserExtensionConfiguration(extensionPath, context);
    verifyExtensionBundle(extensionPath,context);
  }

  private void verifyParserExtensionConfiguration(Path extensionPath, Map<Paths,List<Path>> context) throws Exception{
    // parsers must have configurations
    // config/
    // config/zookeeper/
    // config/zookeeper/enrichments
    // config/zookeeper/indexing
    // config/zookeeper/parsers

    // there may be other, TBD

    Path config = extensionPath.resolve("config");
    if(!config.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing configuration");
    }
    List<Path> configPaths = new ArrayList<>();
    configPaths.add(config);
    context.put(Paths.CONFIG,configPaths);

    Path enrichments = config.resolve("zookeeper/enrichments");
    if(!enrichments.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing Enrichment Configuration ");
    }

    Collection<File> configurations = FileUtils.listFiles(enrichments.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new Exception("Invalid Parser Extension: Missing Enrichment Configuration ");
    }

    List<Path> enrichmentConfigPaths = new ArrayList<>();
    for(File thisConfigFile : configurations){
      enrichmentConfigPaths.add(thisConfigFile.toPath());
    }

    List<Path> zookeeperPaths = new ArrayList<>();
    zookeeperPaths.add(enrichments.getParent());

    List<Path> enrichmentConfigDirPaths = new ArrayList<>();
    enrichmentConfigDirPaths.add(enrichments);

    context.put(Paths.ENRICHMENTS_CONFIG,enrichmentConfigPaths);
    context.put(Paths.ZOOKEEPER,zookeeperPaths);
    context.put(Paths.ENRICHMENTS_CONFIG_DIR,enrichmentConfigDirPaths);

    Path indexing = config.resolve("zookeeper/indexing");
    if(!indexing.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing Indexing Configuration ");
    }
    configurations = FileUtils.listFiles(indexing.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new Exception("Invalid Parser Extension: Missing Indexing Configuration ");
    }


    List<Path> indexingConfigPaths = new ArrayList<>();
    for(File thisConfigFile : configurations){
      indexingConfigPaths.add(thisConfigFile.toPath());
    }

    List<Path> indexingConfigDirPaths = new ArrayList<>();
    indexingConfigDirPaths.add(indexing);
    context.put(Paths.INDEXING_CONFIG,indexingConfigPaths);
    context.put(Paths.INDEXING_CONFIG_DIR,indexingConfigDirPaths);

    Path parsers = config.resolve("zookeeper/parsers");
    if(!parsers.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing Parsers Configuration ");
    }
    configurations = FileUtils.listFiles(parsers.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new Exception("Invalid Parser Extension: Missing Parsers Configuration ");
    }

    List<Path> parserConfigPaths = new ArrayList<>();
    for(File thisConfigFile : configurations){
      parserConfigPaths.add(thisConfigFile.toPath());
    }

    List<Path> parserConfigDirPaths = new ArrayList<>();
    parserConfigDirPaths.add(indexing);

    context.put(Paths.PARSERS_CONFIG,parserConfigPaths);
    context.put(Paths.PARSERS_CONFIG_DIR,parserConfigDirPaths);

  }

  private void verifyExtensionBundle(Path extensionPath, Map<Paths,List<Path>> context) throws Exception{
    // check if there is a bundle at all
    // if there is verify that
    Collection<File> bundles = FileUtils.listFiles(extensionPath.toFile(),BUNDLE_EXT,false);
    if(bundles.isEmpty()){
      // this is a configuration only parser
      // which is ok
      return;
    }
    List<Path> bundlePathList = new ArrayList<>();
    bundlePathList.add(bundles.stream().findFirst().get().toPath());
    context.put(Paths.BUNDLE,bundlePathList);

    // TODO - load the bundle and verify the metadata
    // TODO - get the bundle.properties out of zk to get the correct prefixes etc
  }

  private void saveEnrichmentConfigs(Map<Paths,List<Path>> context , List<String> loadedConfigs) throws Exception{
    for (Map.Entry<Paths,List<Path>> entry : context.entrySet()) {
      if (entry.getKey() == Paths.ENRICHMENTS_CONFIG) {
        for (Path configPath : entry.getValue()) {
          File configFile = configPath.toFile();
          String configName = configFile.getName().substring(0, configFile.getName().lastIndexOf('.'));
          SensorEnrichmentConfig config = SensorEnrichmentConfig.fromBytes(FileUtils.readFileToByteArray(configFile));
          sensorEnrichmentConfigService.save(configName, config);
          loadedConfigs.add(configName);
        }
      }
    }
  }

  private void saveIndexingConfigs(Map<Paths,List<Path>> context, List<String> loadedConfigs) throws Exception{
    for (Map.Entry<Paths,List<Path>> entry : context.entrySet()) {
      if (entry.getKey() == Paths.INDEXING_CONFIG) {
        for (Path configPath : entry.getValue()) {
          File configFile = configPath.toFile();
          String configName = configFile.getName().substring(0, configFile.getName().lastIndexOf('.'));

          Map<String,Object> sensorIndexingConfig = JSONUtils.INSTANCE.load(new ByteArrayInputStream(FileUtils.readFileToByteArray(configFile)), new TypeReference<Map<String, Object>>(){});
          sensorIndexingConfigService.save(configName,sensorIndexingConfig);
          loadedConfigs.add(configName);
        }
      }
    }
  }

  private void saveParserConfigs(Map<Paths,List<Path>> context, List<String> loadedConfigs) throws Exception{
    for (Map.Entry<Paths,List<Path>> entry : context.entrySet()) {
      if (entry.getKey() == Paths.PARSERS_CONFIG) {
        for (Path configPath : entry.getValue()) {
          File configFile = configPath.toFile();
          String configName = configFile.getName().substring(0, configFile.getName().lastIndexOf('.'));
          SensorParserConfig config = SensorParserConfig.fromBytes(FileUtils.readFileToByteArray(configFile));
          sensorParserConfigService.save(config);
          loadedConfigs.add(configName);
        }
      }
    }
  }

  private void deployBundleToHdfs(Map<Paths,List<Path>> context) throws Exception{
    Path bundlePath = context.get(Paths.BUNDLE).get(0);
    // need to get the alt lib path from the bundle
    Optional<BundleProperties> opProperties = getBundleProperties(client);
    if(!opProperties.isPresent()){
      throw new Exception("Bundle Properties are not loaded into system");
    }
    BundleProperties props = opProperties.get();
    org.apache.hadoop.fs.Path altPath = new org.apache.hadoop.fs.Path(props.getProperty("bundle.library.directory.alt"));
    org.apache.hadoop.fs.Path targetPath = new org.apache.hadoop.fs.Path(altPath, bundlePath.toFile().getName());
    hdfsService.write(targetPath,FileUtils.readFileToByteArray(bundlePath.toFile()));
  }

  private static Optional<BundleProperties> getBundleProperties(CuratorFramework client) throws Exception{
    BundleProperties properties = null;
    byte[] propBytes = ConfigurationsUtils.readFromZookeeper(Constants.ZOOKEEPER_ROOT + "/bundle.properties",client);
    if(propBytes.length > 0 ) {
      // read in the properties
      properties = BundleProperties.createBasicBundleProperties(new ByteArrayInputStream(propBytes),new HashMap<>());
    }
    return Optional.of(properties);
  }
}
