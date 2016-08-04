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
package org.apache.metron.maas.service.callback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.metron.maas.config.ModelRequest;
import org.apache.metron.maas.service.runner.Runner;
import org.apache.metron.maas.service.runner.Runner.RunnerOptions;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.*;

public class LaunchContainer implements Runnable {
  private static final Log LOG = LogFactory.getLog(LaunchContainer.class);
  private ModelRequest request;
  private Container container;
  private Configuration conf ;
  private NMClientAsync nmClientAsync;
  private String zkQuorum;
  private String zkRoot;
  private ByteBuffer allTokens;
  private Path appJarLocation;

  public LaunchContainer( Configuration conf
          , String zkQuorum
          , String zkRoot
          , NMClientAsync nmClientAsync
          , ModelRequest request
          , Container container
          , ByteBuffer allTokens
          , Path appJarLocation
  )
  {
    this.allTokens = allTokens;
    this.zkQuorum = zkQuorum;
    this.zkRoot = zkRoot;
    this.request = request;
    this.container = container;
    this.conf = conf;
    this.nmClientAsync = nmClientAsync;
    this.appJarLocation = appJarLocation;
  }

  @Override
  public void run() {
    LOG.info("Setting up container launch container for containerid="
            + container.getId());
    // Set the local resources
    Map<String, LocalResource> localResources = new HashMap<>();
    LOG.info("Local Directory Contents");
    for(File f : new File(".").listFiles()) {
      LOG.info("  " + f.length() + " - " + f.getName() );
    }
    LOG.info("Localizing " + request.getPath());
    String modelScript = localizeResources(localResources, new Path(request.getPath()), appJarLocation);
    for(Map.Entry<String, LocalResource> entry : localResources.entrySet()) {
      LOG.info(entry.getKey() + " localized: " + entry.getValue().getResource() );
    }
    // The container for the eventual shell commands needs its own local
    // resources too.
    // In this scenario, if a shell script is specified, we need to have it
    // copied and made available to the container.

    // Set the necessary command to execute on the allocated container
    Map<String, String> env = new HashMap<>();
    // For example, we could setup the classpath needed.
    // Assuming our classes or jars are available as local resources in the
    // working directory from which the command will be run, we need to append
    // "." to the path.
    // By default, all the hadoop specific classpaths will already be available
    // in $CLASSPATH, so we should be careful not to overwrite it.
    StringBuffer classPathEnv = new StringBuffer("$CLASSPATH:./*:");
    //if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
      classPathEnv.append(System.getProperty("java.class.path"));
    //}
    env.put("CLASSPATH", classPathEnv.toString());
    // Construct the command to be executed on the launched container
    String command = ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java "
            + Runner.class.getName() + " "
            + RunnerOptions.toArgs(RunnerOptions.CONTAINER_ID.of(container.getId().getContainerId() + "")
            ,RunnerOptions.ZK_QUORUM.of(zkQuorum)
            ,RunnerOptions.ZK_ROOT.of(zkRoot)
            ,RunnerOptions.SCRIPT.of(modelScript)
            ,RunnerOptions.NAME.of(request.getName())
            ,RunnerOptions.HOSTNAME.of(containerHostname())
            ,RunnerOptions.VERSION.of(request.getVersion())
    )
            + " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout"
            + " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr";
    List<String> commands = new ArrayList<String>();
    LOG.info("Executing container command: " + command);
    commands.add(command);


    // Set up ContainerLaunchContext, setting local resource, environment,
    // command and token for constructor.

    // Note for tokens: Set up tokens for the container too. Today, for normal
    // shell commands, the container in distribute-shell doesn't need any
    // tokens. We are populating them mainly for NodeManagers to be able to
    // download anyfiles in the distributed file-system. The tokens are
    // otherwise also useful in cases, for e.g., when one is running a
    // "hadoop dfs" command inside the distributed shell.
    ContainerLaunchContext ctx = ContainerLaunchContext.newInstance(
            localResources, env, commands, null, allTokens.duplicate(), null);
    //TODO: Add container to listener so it can be removed
    nmClientAsync.startContainerAsync(container, ctx);
  }

  private Map.Entry<String, LocalResource> localizeResource(FileStatus status) {
    URL url = ConverterUtils.getYarnUrlFromURI( status.getPath().toUri());
    LocalResource resource = LocalResource.newInstance(url,
            LocalResourceType.FILE, LocalResourceVisibility.APPLICATION,
            status.getLen(), status.getModificationTime());
    String name = status.getPath().getName();
    return new AbstractMap.SimpleEntry<>(name, resource);
  }

  public String localizeResources(Map<String, LocalResource> resources, Path scriptLocation, Path appJarLocation) {
    try {
      LOG.info("Model payload: " + scriptLocation);
      LOG.info("AppJAR Location: " + appJarLocation);
      FileSystem fs = scriptLocation.getFileSystem(conf);
      String script = null;
      Map.Entry<String, LocalResource> kv = localizeResource(fs.getFileStatus(appJarLocation));
      resources.put(kv.getKey(), kv.getValue());
      for (RemoteIterator<LocatedFileStatus> it = fs.listFiles(scriptLocation, true);it.hasNext();) {
        LocatedFileStatus status = it.next();
        kv = localizeResource(status);
        String name = kv.getKey();
        if(name.endsWith(".sh")) {
          script = name;
        }
        LOG.info("Localized " + name + " -> " + status.toString());
        resources.put(name, kv.getValue());
      }
      return script;
    }
    catch(Exception e) {
      LOG.error(e.getMessage(), e);
      return null;
    }
  }
  private String containerHostname() {
    String nodeHost = null;
    try {
      boolean hasProtocol = container.getNodeHttpAddress().startsWith("http");
      java.net.URL nodehttpAddress = new java.net.URL((hasProtocol?"":"http://") + container.getNodeHttpAddress());
      nodeHost = nodehttpAddress.getHost();
    } catch (MalformedURLException e) {
      LOG.error(e.getMessage(), e);
      throw new IllegalStateException("Unable to parse " + container.getNodeHttpAddress() + " into a URL");
    }
    return nodeHost;
  }
}
