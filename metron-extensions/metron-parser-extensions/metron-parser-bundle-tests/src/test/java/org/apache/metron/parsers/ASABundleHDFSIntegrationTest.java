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
package org.apache.metron.parsers;


import com.google.common.base.Function;
import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.metron.TestConstants;
import org.apache.metron.bundles.BundleClassLoaders;
import org.apache.metron.bundles.ExtensionClassInitializer;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileUtilities;
import org.apache.metron.bundles.util.FileUtils;
import org.apache.metron.bundles.util.HDFSFileUtilities;
import org.apache.metron.common.Constants;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.MRComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.integration.processors.KafkaMessageSet;
import org.apache.metron.integration.processors.KafkaProcessor;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.parsers.integration.ParserValidation;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.parsers.integration.validation.PathedSampleDataValidation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ASABundleHDFSIntegrationTest extends BaseIntegrationTest {
  static final Map<String,String> EMPTY_MAP = new HashMap<String,String>();
  static final String sensorType = "asa";
  static final String ERROR_TOPIC = "parser_error";
  protected List<byte[]> inputMessages;
  @AfterClass
  public static void after(){
    try {
      RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(new Path("/"), true);
      System.out.println("==============(AFTER)==============");
      while (files.hasNext()) {
        LocatedFileStatus fileStat = files.next();
        System.out.println(fileStat.getPath().toString());
      }
    }catch(Exception e){}
    mrComponent.stop();
    ExtensionClassInitializer.reset();
    BundleClassLoaders.reset();
    FileUtils.reset();
  }

  static MRComponent mrComponent;
  static Configuration configuration;
  static FileSystem fileSystem;
  @BeforeClass
  public static void setup() {
    mrComponent = new MRComponent().withBasePath("target/hdfs");
    mrComponent.start();
    configuration = mrComponent.getConfiguration();

    try {

      // copy the correct things in
      copyResources("./src/test/resources","./target/remote");

      // we need to patch the properties file
      BundleProperties properties = BundleProperties.createBasicBundleProperties("./target/remote/zookeeper/bundle.properties",new HashMap<>());
      properties.setProperty(BundleProperties.HDFS_PREFIX,configuration.get("fs.defaultFS"));
      properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY, "/extension_lib/");
      properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY_PREFIX + "alt", "/extension_contrib_lib/");
      properties.setProperty(BundleProperties.BUNDLE_WORKING_DIRECTORY,"/work/");
      properties.setProperty(BundleProperties.COMPONENT_DOCS_DIRECTORY,"/work/docs/components/");
      FileOutputStream fso = new FileOutputStream("./target/remote/zookeeper/bundle.properties");
      properties.storeProperties(fso,"HDFS UPDATE");
      fso.flush();
      fso.close();

      fileSystem = FileSystem.newInstance(configuration);
      if(!fileSystem.mkdirs(new Path("/work/"),new FsPermission(FsAction.READ_WRITE,FsAction.READ_WRITE,FsAction.READ_WRITE))){
        System.out.println("FAILED MAKE DIR");
      }
      fileSystem.copyFromLocalFile(new Path("./target/remote/metron/extension_contrib_lib/"), new Path("/"));
      fileSystem.copyFromLocalFile(new Path("./target/remote/metron/extension_lib/"), new Path("/"));
      fileSystem.copyFromLocalFile(new Path("./target/remote/zookeeper/bundle.properties"), new Path("/work/"));

      RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(new Path("/"),true);
      System.out.println("==============(BEFORE)==============");
      while (files.hasNext()){
        LocatedFileStatus fileStat = files.next();
        System.out.println(fileStat.getPath().toString());
      }

    } catch (IOException e) {
      throw new RuntimeException("Unable to start cluster", e);
    }

  }
  public static void copyResources(String source, String target) throws IOException {
    final java.nio.file.Path sourcePath = Paths.get(source);
    final java.nio.file.Path targetPath = Paths.get(target);

    Files.walkFileTree(sourcePath, new SimpleFileVisitor<java.nio.file.Path>() {

      @Override
      public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs)
              throws IOException {

        java.nio.file.Path relativeSource = sourcePath.relativize(dir);
        java.nio.file.Path target = targetPath.resolve(relativeSource);

        if(!Files.exists(target)) {
          Files.createDirectories(target);
        }
        return FileVisitResult.CONTINUE;

      }

      @Override
      public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs)
              throws IOException {

        java.nio.file.Path relativeSource = sourcePath.relativize(file);
        java.nio.file.Path target = targetPath.resolve(relativeSource);

        Files.copy(file, target, REPLACE_EXISTING);

        return FileVisitResult.CONTINUE;
      }
    });
  }

  @Test
  public void testHDFS() throws Exception{
    final Properties topologyProperties = new Properties();
    inputMessages = TestUtils.readSampleData(getSampleDataPath());
    final KafkaComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaComponent.Topic>() {{
      add(new KafkaComponent.Topic(sensorType, 1));
      add(new KafkaComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
      add(new KafkaComponent.Topic(ERROR_TOPIC,1));
    }});
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    ZKServerComponent zkServerComponent = getZKServerComponent(topologyProperties);

    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfigsPath("./target/remote/zookeeper/")
            .withParserConfigsPath("../metron-parser-asa-extension/metron-parser-asa/" + TestConstants.THIS_PARSER_CONFIGS_PATH);

    ParserTopologyComponent parserTopologyComponent = new ParserTopologyComponent.Builder()
            .withSensorType(sensorType)
            .withTopologyProperties(topologyProperties)
            .withBrokerUrl(kafkaComponent.getBrokerList()).build();

    //UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("zk", zkServerComponent)
            .withComponent("kafka", kafkaComponent)
            .withComponent("config", configUploadComponent)
            .withComponent("org/apache/storm", parserTopologyComponent)
            .withMillisecondsBetweenAttempts(5000)
            .withNumRetries(10)
            .withCustomShutdownOrder(new String[] {"org/apache/storm","config","kafka","zk"})
            .build();
    runner.start();

    try {
      kafkaComponent.writeMessages(sensorType, inputMessages);
      ProcessorResult<List<byte[]>> result = runner.process(getProcessor());
      List<byte[]> outputMessages = result.getResult();
      StringBuffer buffer = new StringBuffer();
      if (result.failed()){
        result.getBadResults(buffer);
        buffer.append(String.format("%d Valid Messages Processed", outputMessages.size())).append("\n");
        dumpParsedMessages(outputMessages,buffer);
        Assert.fail(buffer.toString());
      } else {
        List<ParserValidation> validations = getValidations();
        if (validations == null || validations.isEmpty()) {
          buffer.append("No validations configured for sensorType " + sensorType + ".  Dumping parsed messages").append("\n");
          dumpParsedMessages(outputMessages,buffer);
          Assert.fail(buffer.toString());
        } else {
          for (ParserValidation validation : validations) {
            System.out.println("Running " + validation.getName() + " on sensorType " + sensorType);
            validation.validate(sensorType, outputMessages);
          }
        }
      }
    } finally {
      runner.stop();
    }
  }

  public void dumpParsedMessages(List<byte[]> outputMessages, StringBuffer buffer) {
    for (byte[] outputMessage : outputMessages) {
      buffer.append(new String(outputMessage)).append("\n");
    }
  }

  @SuppressWarnings("unchecked")
  private KafkaProcessor<List<byte[]>> getProcessor(){

    return new KafkaProcessor<>()
            .withKafkaComponentName("kafka")
            .withReadTopic(Constants.ENRICHMENT_TOPIC)
            .withErrorTopic(ERROR_TOPIC)
            .withValidateReadMessages(new Function<KafkaMessageSet, Boolean>() {
              @Nullable
              @Override
              public Boolean apply(@Nullable KafkaMessageSet messageSet) {
                return (messageSet.getMessages().size() + messageSet.getErrors().size() == inputMessages.size());
              }
            })
            .withProvideResult(new Function<KafkaMessageSet,List<byte[]>>(){
              @Nullable
              @Override
              public List<byte[]> apply(@Nullable KafkaMessageSet messageSet) {
                return messageSet.getMessages();
              }
            });
  }

  public List<ParserValidation> getValidations() {
    return new ArrayList<ParserValidation>() {{
      add(new PathedSampleDataValidation("../metron-parser-asa-extension/metron-parser-asa/src/test/resources/data/parsed/test.parsed"));
    }};
  }

  protected String getGlobalConfigPath() throws Exception{
    return "../../../../metron-platform/metron-integration-test/src/main/config/zookeeper/";
  }

  protected String getSampleDataPath() throws Exception {
    return "../metron-parser-asa-extension/metron-parser-asa/src/test/resources/data/raw/test.raw";
  }
}
