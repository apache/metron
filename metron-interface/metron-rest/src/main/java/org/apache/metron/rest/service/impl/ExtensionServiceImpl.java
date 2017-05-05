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
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.extensions.ParserExtensionConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.guava.io.Files;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.*;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Service
public class ExtensionServiceImpl implements ExtensionService{
  final static int BUFFER_SIZ = 2048;
  final static String[] CONFIG_EXT = {"json"};
  final static String[] BUNDLE_EXT = {"bundle"};
  final static String[] ES_EXT = {"template"};

  @Autowired
  HdfsService hdfsService;
  @Autowired
  KafkaService kafkaService;
  @Autowired
  SensorEnrichmentConfigService sensorEnrichmentConfigService;
  @Autowired
  SensorIndexingConfigService sensorIndexingConfigService;
  @Autowired
  SensorParserConfigService sensorParserConfigService;
  @Autowired
  StormAdminService stormAdminService;

  private CuratorFramework client;

  @Autowired
  public ExtensionServiceImpl(CuratorFramework client) {
    this.client = client;
  }

  @Override
  public void install(ExtensionType extensionType, String extensionPackageName, TarArchiveInputStream tgzStream) throws Exception{
     // unpack
     Path unPackedPath = unpackExtension(tgzStream);

     if(extensionType == ExtensionType.PARSER){
       installParserExtension(unPackedPath, extensionPackageName);
     }
     // stellar etc....
  }
  @Override
  public ParserExtensionConfig findOneParserExtension(String name) throws RestException{
    ParserExtensionConfig parserExtensionConfig;
    try {
      parserExtensionConfig = ConfigurationsUtils.readParserExtensionConfigFromZookeeper(name, client);
    } catch (KeeperException.NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new RestException(e);
    }
    return parserExtensionConfig;
  }

  @Override
  public Map<String, ParserExtensionConfig> getAllParserExtensions() throws RestException{
    Map<String, ParserExtensionConfig> parserExtensionConfigs = new HashMap<>();
    List<String> sensorNames = getAllParserExtensionTypes();
    for (String name : sensorNames) {
      parserExtensionConfigs.put(name, findOneParserExtension(name));
    }
    return parserExtensionConfigs;
  }

  @Override
  public List<String> getAllParserExtensionTypes() throws RestException {
    List<String> types;
    try {
      types = client.getChildren().forPath(ConfigurationType.PARSER_EXTENSION.getZookeeperRoot());
    } catch (KeeperException.NoNodeException e) {
      types = new ArrayList<>();
    } catch (Exception e) {
      throw new RestException(e);
    }
    return types;
  }

  @Override
  public boolean deleteParserExtension(String name) throws Exception{
    // in order to delete the parser extension, we need to
    // find the extension, get the parsers for it
    ParserExtensionConfig parserExtensionConfig;
    parserExtensionConfig = findOneParserExtension(name);
    Collection<String> parsers = parserExtensionConfig.getParserExtensionParserNames();
    for(String parser : parsers){
      // NOTE if any one parser fails, then we will not continue
      // we may continue and then we have to think through a more
      // complicated failure mode
      stormAdminService.stopParserTopology(parser, true);
      kafkaService.deleteTopic(parser);
      // should we delete them?
      //deleteGrokRulesFromHdfs(parser);
      sensorEnrichmentConfigService.delete(parser);
      sensorIndexingConfigService.delete(parser);
      sensorParserConfigService.delete(parser);
    }

    deleteBundleFromHdfs(parserExtensionConfig.getExtensionBundleName());
    ConfigurationsUtils.deleteParsesrExtensionConfig(name, client);
    return true;
  }

  private void installParserExtension(Path extensionPath, String extentionPackageName) throws Exception{
    final InstallContext context = new InstallContext();
    context.extensionPackageName = Optional.of(formatPackageName(extentionPackageName));
    context.bundleProperties = loadBundleProperties();

    // verify the structure
    verifyParserExtension(extensionPath, context);

    // after verification we will have all the paths we need
    // we want to keep a list of things we have pushed to zk, so if one fails we can remove them and 'roll back'
    // we only need to pass the name to the delete
    // we do the hdfs deploy last, so we should be able to avoid deleting it after lay down
    final List<String> loadedEnrichementConfigs = new ArrayList<>();
    final List<String> loadedIndexingConfigs = new ArrayList<>();
    final List<String> loadedParserConfigs = new ArrayList<>();
    final List<String> loadedElasticSearchTemplates = new ArrayList<>();
    try{
        saveEnrichmentConfigs(context, loadedEnrichementConfigs);
        saveIndexingConfigs(context, loadedIndexingConfigs);
        saveParserConfigs(context, loadedParserConfigs);
        deployGrokRulesToHdfs(context);
        deployBundleToHdfs(context);
        writeExtensionConfiguration(context);
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
        // double check that the parent exists
        if(!childPath.getParent().toFile().exists()){
          childPath.getParent().toFile().mkdirs();
        }
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

  private void verifyParserExtension(Path extensionPath, InstallContext context) throws Exception{
    verifyParserExtensionConfiguration(extensionPath, context);
    verifyExtensionBundle(extensionPath,context);
  }

  private void verifyParserExtensionConfiguration(Path extensionPath, InstallContext context) throws Exception{
    // parsers must have configurations
    // config/
    // config/zookeeper/
    // config/zookeeper/enrichments
    // config/zookeeper/indexing
    // config/zookeeper/parsers
    // config/zookeeper/elasticsearch

    // there may be other, TBD

    Path configPath = extensionPath.resolve("config");
    if(!configPath.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing configuration");
    }
    List<Path> configPaths = new ArrayList<>();
    configPaths.add(configPath);
    context.pathContext.put(Paths.CONFIG,configPaths);

    Path patterns = extensionPath.resolve("patterns");
    if(patterns.toFile().exists()) {
      List<Path> patternsList = new ArrayList<>();
      patternsList.add(patterns);
      context.pathContext.put(Paths.GROK_DIR, patternsList);


      File[] grockRuleFiles = patterns.toFile().listFiles();
      if (grockRuleFiles.length != 0) {
        List<Path> grokRulePaths = new ArrayList<>();
        for (File thisConfigFile : grockRuleFiles) {
          grokRulePaths.add(thisConfigFile.toPath());
        }
        context.pathContext.put(Paths.GROK_RULES, grokRulePaths);
      }
    }

    Path elasticsearch = configPath.resolve("elasticsearch");
    if(elasticsearch.toFile().exists()) {
      List<Path> esList = new ArrayList<>();
      esList.add(elasticsearch);
      context.pathContext.put(Paths.ELASTICSEARCH_DIR, esList);

      Collection<File> esTemplates = FileUtils.listFiles(elasticsearch.toFile(), ES_EXT, false);
      Map<String, Map<String, Object>> defaultElasticSearchTemplates = new HashMap<>();
      if (!esTemplates.isEmpty()) {
        List<Path> esTemplatePaths = new ArrayList<>();
        for (File thisTemplateFile : esTemplates) {
          esTemplatePaths.add(thisTemplateFile.toPath());
          Map<String, Object> esTemplate = JSONUtils.INSTANCE.load(new ByteArrayInputStream(Files.toByteArray(thisTemplateFile)), new TypeReference<Map<String, Object>>() {
          });
          defaultElasticSearchTemplates.put(thisTemplateFile.getName(), esTemplate);
        }
        context.pathContext.put(Paths.ELASTICSEARCH_TEMPLATES, esTemplatePaths);
        context.defaultElasticSearchTemplates = Optional.of(defaultElasticSearchTemplates);
      }
    }

    Path enrichments = configPath.resolve("zookeeper/enrichments");
    if(!enrichments.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing Enrichment Configuration ");
    }

    Collection<File> configurations = FileUtils.listFiles(enrichments.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new Exception("Invalid Parser Extension: Missing Enrichment Configuration ");
    }

    List<Path> enrichmentConfigPaths = new ArrayList<>();
    Map<String,SensorEnrichmentConfig> defaultEnrichmentConfigs = new HashMap<>();
    for(File thisConfigFile : configurations){
      enrichmentConfigPaths.add(thisConfigFile.toPath());
      String name = thisConfigFile.getName().substring(0,thisConfigFile.getName().lastIndexOf('.'));
      SensorEnrichmentConfig enrichmentConfig = SensorEnrichmentConfig.fromBytes(Files.toByteArray(thisConfigFile));
      defaultEnrichmentConfigs.put(name,enrichmentConfig);
    }
    context.defaultEnrichmentConfigs = Optional.of(defaultEnrichmentConfigs);

    List<Path> zookeeperPaths = new ArrayList<>();
    zookeeperPaths.add(enrichments.getParent());

    List<Path> enrichmentConfigDirPaths = new ArrayList<>();
    enrichmentConfigDirPaths.add(enrichments);

    context.pathContext.put(Paths.ENRICHMENTS_CONFIG,enrichmentConfigPaths);
    context.pathContext.put(Paths.ZOOKEEPER,zookeeperPaths);
    context.pathContext.put(Paths.ENRICHMENTS_CONFIG_DIR,enrichmentConfigDirPaths);

    Path indexing = configPath.resolve("zookeeper/indexing");
    if(!indexing.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing Indexing Configuration ");
    }
    configurations = FileUtils.listFiles(indexing.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new Exception("Invalid Parser Extension: Missing Indexing Configuration ");
    }


    List<Path> indexingConfigPaths = new ArrayList<>();
    Map<String,Map<String,Object>> defaultIndexingConfigs = new HashMap<>();
    for(File thisConfigFile : configurations){
      indexingConfigPaths.add(thisConfigFile.toPath());
      String name = thisConfigFile.getName().substring(0,thisConfigFile.getName().lastIndexOf('.'));
      Map<String,Object> indexingConfig = JSONUtils.INSTANCE.load(new ByteArrayInputStream(Files.toByteArray(thisConfigFile)), new TypeReference<Map<String, Object>>() {});
      defaultIndexingConfigs.put(name,indexingConfig);
    }
    context.defaultIndexingConfigs = Optional.of(defaultIndexingConfigs);
    List<Path> indexingConfigDirPaths = new ArrayList<>();
    indexingConfigDirPaths.add(indexing);
    context.pathContext.put(Paths.INDEXING_CONFIG,indexingConfigPaths);
    context.pathContext.put(Paths.INDEXING_CONFIG_DIR,indexingConfigDirPaths);

    Path parsers = configPath.resolve("zookeeper/parsers");
    if(!parsers.toFile().exists()){
      throw new Exception("Invalid Parser Extension: Missing Parsers Configuration ");
    }
    configurations = FileUtils.listFiles(parsers.toFile(),CONFIG_EXT,false);
    if(configurations.isEmpty()){
      throw new Exception("Invalid Parser Extension: Missing Parsers Configuration ");
    }

    List<Path> parserConfigPaths = new ArrayList<>();
    Map<String,SensorParserConfig> defaultParserConfigs = new HashMap<>();
    for(File thisConfigFile : configurations){
      parserConfigPaths.add(thisConfigFile.toPath());
      String parserName = thisConfigFile.getName().substring(0,thisConfigFile.getName().lastIndexOf('.'));
      context.extensionParserNames.add(parserName);
      SensorParserConfig defaultParserConfig = SensorParserConfig.fromBytes(Files.toByteArray(thisConfigFile));
      defaultParserConfigs.put(parserName,defaultParserConfig);
    }

    context.defaultParserConfigs = Optional.of(defaultParserConfigs);

    List<Path> parserConfigDirPaths = new ArrayList<>();
    parserConfigDirPaths.add(indexing);

    context.pathContext.put(Paths.PARSERS_CONFIG,parserConfigPaths);
    context.pathContext.put(Paths.PARSERS_CONFIG_DIR,parserConfigDirPaths);

  }

  private void verifyExtensionBundle(Path extensionPath, InstallContext context) throws Exception{
    // check if there is a bundle at all
    // if there is verify that
    Path libPath = extensionPath.resolve("lib");
    if(libPath.toFile().exists() == false){
      return;
    }

    Collection<File> bundles = FileUtils.listFiles(libPath.toFile(),BUNDLE_EXT,false);
    if(bundles.isEmpty()){
      // this is a configuration only parser
      // which is ok
      return;
    }
    List<Path> bundlePathList = new ArrayList<>();
    Path bundlePath = bundles.stream().findFirst().get().toPath();
    bundlePathList.add(bundlePath);
    context.pathContext.put(Paths.BUNDLE,bundlePathList);
    context.bundleName = Optional.of(bundlePath.toFile().getName());
    try (final JarFile bundle = new JarFile(bundlePath.toFile())) {
      final Manifest manifest = bundle.getManifest();
      final Attributes attributes = manifest.getMainAttributes();
      final String bundleId = attributes.getValue(String.format("%s-Id",context.bundleProperties.get().getMetaIdPrefix()));
      final String bundleVersion = attributes.getValue(String.format("%s-Version",context.bundleProperties.get().getMetaIdPrefix()));
      context.bundleID = Optional.of(bundleId);
      context.bundleVersion = Optional.of(bundleVersion);
    }
  }

  private void saveEnrichmentConfigs(InstallContext context , List<String> loadedConfigs) throws Exception{
    for (Map.Entry<Paths,List<Path>> entry : context.pathContext.entrySet()) {
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

  private void saveIndexingConfigs(InstallContext context, List<String> loadedConfigs) throws Exception{
    for (Map.Entry<Paths,List<Path>> entry : context.pathContext.entrySet()) {
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

  private void saveParserConfigs(InstallContext context, List<String> loadedConfigs) throws Exception{
    for (Map.Entry<Paths,List<Path>> entry : context.pathContext.entrySet()) {
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

  private void deployBundleToHdfs(InstallContext context) throws Exception{
    List<Path> bundlePaths = context.pathContext.get(Paths.BUNDLE);
    if(bundlePaths == null || bundlePaths.size() == 0){
      return;
    }

    Path bundlePath = bundlePaths.get(0);

    BundleProperties props = context.bundleProperties.get();
    org.apache.hadoop.fs.Path altPath = new org.apache.hadoop.fs.Path(props.getProperty("bundle.library.directory.alt"));
    org.apache.hadoop.fs.Path targetPath = new org.apache.hadoop.fs.Path(altPath, bundlePath.toFile().getName());
    hdfsService.write(targetPath,FileUtils.readFileToByteArray(bundlePath.toFile()));
  }

  private void deleteBundleFromHdfs(String bundleName) throws Exception{
    Optional<BundleProperties> optionalProperties = getBundleProperties(client);
    if(!optionalProperties.isPresent()){
      throw new Exception("Failed to retrieve Bundle.Properties, unknown error");
    }
    BundleProperties props = optionalProperties.get();
    org.apache.hadoop.fs.Path altPath = new org.apache.hadoop.fs.Path(props.getProperty("bundle.library.directory.alt"));

    if(hdfsService.list(altPath).contains(bundleName)){
      org.apache.hadoop.fs.Path targetPath = new org.apache.hadoop.fs.Path(altPath, bundleName);
      hdfsService.delete(targetPath, false);
    }
  }


  private void deployGrokRulesToHdfs(InstallContext context)throws Exception{
    List<Path> grokRulePaths = context.pathContext.get(Paths.GROK_RULES);
    if(grokRulePaths == null || grokRulePaths.size() == 0){
      return;
    }

    // Not sure how we get the path here, it is a var set in ambari
    // TODO: replace this with a 'correct' lookup of the root
    // also copy of directory would be better here
    // Rules are shared across all parsers in a given extension assembly
    String hardCodedHdfsPatterns = "/apps/metron/patterns";
    Optional<BundleProperties> optionalProps = loadBundleProperties();
    if(optionalProps.isPresent()){
      String override = optionalProps.get().getProperty("testing.hdfs.patterns.dir.override");
      if(org.apache.commons.lang.StringUtils.isNotEmpty(override)){
        hardCodedHdfsPatterns = override;
      }
    }

    org.apache.hadoop.fs.Path patternPath = new org.apache.hadoop.fs.Path(hardCodedHdfsPatterns);
    hdfsService.ensureDirectory(patternPath);
    for(String parserName : context.extensionParserNames) {
      org.apache.hadoop.fs.Path parserRulePath = new org.apache.hadoop.fs.Path(patternPath, parserName);
      for(Path thisRule : grokRulePaths){
        org.apache.hadoop.fs.Path targetPath = new org.apache.hadoop.fs.Path(parserRulePath,thisRule.toFile().getName());
        hdfsService.write(targetPath,FileUtils.readFileToByteArray(thisRule.toFile()));
      }
    }

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

  private Optional<BundleProperties> loadBundleProperties(){
    try {
      return getBundleProperties(client);
    }catch (Exception e){
      return Optional.empty();
    }
  }

  private void writeExtensionConfiguration(InstallContext context) throws Exception {
    ParserExtensionConfig config = new ParserExtensionConfig();
    config.setParserExtensionParserName(context.extensionParserNames);
    config.setExtensionsBundleID(context.bundleID.get());
    config.setExtensionsBundleVersion(context.bundleVersion.get());
    config.setExtensionBundleName(context.bundleName.get());
    config.setExtensionAssemblyName(context.extensionPackageName.get());
    config.setDefaultParserConfigs(context.defaultParserConfigs.get());
    config.setDefaultEnrichementConfigs(context.defaultEnrichmentConfigs.get());
    config.setDefaultIndexingConfigs(context.defaultIndexingConfigs.get());
    if(context.defaultElasticSearchTemplates.isPresent()) {
      config.setDefaultElasticSearchTemplates(context.defaultElasticSearchTemplates.get());
    }
    ConfigurationsUtils.writeParserExtensionConfigToZookeeper(context.extensionPackageName.get(),config.toJSON().getBytes(), client);
  }
  @Override
  public String formatPackageName(String name){
    return name.substring(0,name.lastIndexOf("-archive.tar.gz")).replace('.','_');
  }

  class InstallContext {
    public Map<Paths,List<Path>> pathContext = new HashMap<>();
    public Set<String> extensionParserNames = new HashSet<>();
    public Optional<String> bundleID = Optional.empty();
    public Optional<String> bundleVersion = Optional.empty();
    public Optional<String> bundleName = Optional.empty();
    public Optional<BundleProperties> bundleProperties = Optional.empty();
    public Optional<String> extensionPackageName = Optional.empty();
    public Optional<Map<String,SensorParserConfig>> defaultParserConfigs = Optional.empty();
    public Optional<Map<String,SensorEnrichmentConfig>> defaultEnrichmentConfigs = Optional.empty();
    public Optional<Map<String,Map<String,Object>>> defaultIndexingConfigs = Optional.empty();
    public Optional<Map<String,Map<String,Object>>> defaultElasticSearchTemplates = Optional.empty();
  }
}
