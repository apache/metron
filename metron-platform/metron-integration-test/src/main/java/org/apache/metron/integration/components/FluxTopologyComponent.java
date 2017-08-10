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
package org.apache.metron.integration.components;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.flux.FluxBuilder;
import org.apache.storm.flux.model.ExecutionContext;
import org.apache.storm.flux.model.TopologyDef;
import org.apache.storm.flux.parser.FluxParser;
import org.apache.storm.generated.KillOptions;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.thrift.TException;
import org.apache.zookeeper.data.Stat;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FluxTopologyComponent implements InMemoryComponent {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  LocalCluster stormCluster;
  String topologyName;
  File topologyLocation;
  File templateLocation;
  Properties topologyProperties;

  public static class Builder {

    String topologyName;
    File topologyLocation;
    File templateLocation;
    Properties topologyProperties;

    public Builder withTopologyName(String name) {
      this.topologyName = name;
      return this;
    }

    public Builder withTopologyLocation(File location) {
      this.topologyLocation = location;
      return this;
    }

    public Builder withTemplateLocation(File location) {
      this.templateLocation = location;
      return this;
    }

    public Builder withTopologyProperties(Properties properties) {
      this.topologyProperties = properties;
      this.topologyProperties.put("storm.home", "target");
      return this;
    }

    public FluxTopologyComponent build() {
      return new FluxTopologyComponent(topologyName, topologyLocation, templateLocation, topologyProperties);
    }
  }

  public FluxTopologyComponent(String topologyName, File topologyLocation, File templateLocation, Properties topologyProperties) {
    this.topologyName = topologyName;
    this.topologyLocation = topologyLocation;
    this.templateLocation = templateLocation;
    this.topologyProperties = topologyProperties;
  }

  public LocalCluster getStormCluster() {
    return stormCluster;
  }

  public String getTopologyName() {
    return topologyName;
  }

  public File getTopologyLocation() {
    return topologyLocation;
  }

  public File getTemplateLocation() {
    return templateLocation;
  }

  public Properties getTopologyProperties() {
    return topologyProperties;
  }

  public String getZookeeperConnectString() {
    return "localhost:2000";
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      stormCluster = new LocalCluster();
      RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
      try(CuratorFramework client = CuratorFrameworkFactory.newClient(getZookeeperConnectString(), retryPolicy)){
        client.start();
        String root = "/storm/leader-lock";
        Stat exists = client.checkExists().forPath(root);
        if(exists == null) {
          client.create().creatingParentsIfNeeded().forPath(root);
        }
      }
      catch(Exception e) {
        LOG.error("Unable to create leaderlock", e);
      }
      finally {

      }
    } catch (Exception e) {
      throw new UnableToStartException("Unable to start flux topology: " + getTopologyLocation(), e);
    }
  }

  public static void cleanupWorkerDir() {
    if(new File("logs/workers-artifacts").exists()) {
      Path rootPath = Paths.get("logs");
      Path destPath = Paths.get("target/logs");
      try {
        Files.move(rootPath, destPath);
        Files.walk(destPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      } catch (IOException e) {
        throw new IllegalStateException(e.getMessage(), e);
      }
    }
  }

  @Override
  public void stop() {
    if (stormCluster != null) {
      try {
          try {
            // Kill the topology directly instead of sitting through the wait period
            killTopology();
            stormCluster.shutdown();
          } catch (IllegalStateException ise) {
            if (!(ise.getMessage().contains("It took over") && ise.getMessage().contains("to shut down slot"))) {
              throw ise;
            }
            else {
              LOG.error("Attempting to assassinate slots");
              assassinateSlots();
              LOG.error("Storm slots didn't shut down entirely cleanly *sigh*.  " +
                      "I gave them the old one-two-skadoo and killed the slots with prejudice.  " +
                      "If tests fail, we'll have to find a better way of killing them.", ise);
            }
        }
      }
      catch(Throwable t) {
        LOG.error(t.getMessage(), t);
      }
      finally {
        cleanupWorkerDir();
      }
    }
  }

  @Override
  public void reset() {
    if (stormCluster != null) {
      killTopology();
    }
  }

  protected void killTopology() {
    KillOptions ko = new KillOptions();
    ko.set_wait_secs(0);
    stormCluster.killTopologyWithOpts(topologyName, ko);
    try {
      // Actually wait for it to die.
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // Do nothing
    }
  }

  public static void assassinateSlots() {
    /*
    You might be wondering why I'm not just casting to slot here, but that's because the Slot class moved locations
    and we're supporting multiple versions of storm.
     */
    LOG.error("During slot assassination, all candidate threads: {}", Thread.getAllStackTraces().keySet());
    Thread.getAllStackTraces().keySet().stream().filter(t -> t instanceof AutoCloseable && t.getName().toLowerCase().contains("slot")).forEach(t -> {
      LOG.error("Attempting to close thread: " + t + " with state: " + t.getState());
      // With extreme prejudice.  Safety doesn't matter
      try {
        t.stop();
        LOG.error("Called thread.stop() on {}. State is: {}", t.getName(), t.getState());
      } catch(Exception e) {
        // Just swallow anything arising from the threads being killed.
      }
    });
  }

  public void submitTopology() throws NoSuchMethodException, IOException, InstantiationException, TException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException {
    startTopology(getTopologyName(), getTopologyLocation(), getTemplateLocation(), getTopologyProperties());
  }

  private void startTopology(String topologyName, File topologyLoc, File templateFile, Properties properties) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, TException, NoSuchFieldException{
    TopologyDef topologyDef = loadYaml(topologyName, topologyLoc, templateFile, properties);
    Config conf = FluxBuilder.buildConfig(topologyDef);
    ExecutionContext context = new ExecutionContext(topologyDef, conf);
    StormTopology topology = FluxBuilder.buildTopology(context);
    Assert.assertNotNull(topology);
    topology.validate();
    try {
      stormCluster.submitTopology(topologyName, conf, topology);
    }
    catch(Exception nne) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
      stormCluster.submitTopology(topologyName, conf, topology);
    }
  }

  private static TopologyDef loadYaml(String topologyName, File yamlFile, File templateFile, Properties properties) throws IOException {
    File tmpFile = File.createTempFile(topologyName, "props");
    tmpFile.deleteOnExit();
    if (templateFile != null) {
      try (FileWriter propWriter = new FileWriter(tmpFile)){
        String templateContents = FileUtils.readFileToString(templateFile);
        for(Map.Entry prop: properties.entrySet()) {
          String replacePattern = String.format("{{%s}}", prop.getKey());
          templateContents = templateContents.replaceAll(Pattern.quote(replacePattern), (String) prop.getValue());
        }
        propWriter.write(templateContents);
        propWriter.flush();
        return FluxParser.parseFile(yamlFile.getAbsolutePath(), false, true, tmpFile.getAbsolutePath(), false);
      }
    } else {
      try (FileWriter propWriter = new FileWriter(tmpFile)){
        properties.store(propWriter, topologyName + " properties");
        return FluxParser.parseFile(yamlFile.getAbsolutePath(), false, true, tmpFile.getAbsolutePath(), false);
      }
    }

  }
}
