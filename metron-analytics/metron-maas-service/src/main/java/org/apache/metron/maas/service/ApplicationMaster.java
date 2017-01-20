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
package org.apache.metron.maas.service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.ApplicationMasterProtocol;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateRequest;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationMasterRequest;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.TimelineClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.log4j.LogManager;

import com.google.common.annotations.VisibleForTesting;
import org.apache.metron.maas.service.callback.LaunchContainer;
import org.apache.metron.maas.config.Action;
import org.apache.metron.maas.config.ModelRequest;
import org.apache.metron.maas.queue.Queue;
import org.apache.metron.maas.service.callback.ContainerRequestListener;
import org.apache.metron.maas.queue.ZKQueue;
import org.apache.metron.maas.service.runner.MaaSHandler;
import org.apache.metron.maas.service.yarn.Resources;
import org.apache.metron.maas.service.yarn.YarnUtils;

/**
 * An ApplicationMaster for executing shell commands on a set of launched
 * containers using the YARN framework.
 *
 * <p>
 * This class is meant to act as an example on how to write yarn-based
 * application masters.
 * </p>
 *
 * <p>
 * The ApplicationMaster is started on a container by the
 * <code>ResourceManager</code>'s launcher. The first thing that the
 * <code>ApplicationMaster</code> needs to do is to connect and register itself
 * with the <code>ResourceManager</code>. The registration sets up information
 * within the <code>ResourceManager</code> regarding what host:port the
 * ApplicationMaster is listening on to provide any form of functionality to a
 * client as well as a tracking url that a client can use to keep track of
 * status/job history if needed. However, in the distributedshell, trackingurl
 * and appMasterHost:appMasterRpcPort are not supported.
 * </p>
 *
 * <p>
 * The <code>ApplicationMaster</code> needs to send a heartbeat to the
 * <code>ResourceManager</code> at regular intervals to inform the
 * <code>ResourceManager</code> that it is up and alive. The
 * {@link ApplicationMasterProtocol#allocate} to the <code>ResourceManager</code> from the
 * <code>ApplicationMaster</code> acts as a heartbeat.
 *
 * <p>
 * For the actual handling of the job, the <code>ApplicationMaster</code> has to
 * request the <code>ResourceManager</code> via {@link AllocateRequest} for the
 * required no. of containers using {@link ResourceRequest} with the necessary
 * resource specifications such as node location, computational
 * (memory/disk/cpu) resource requirements. The <code>ResourceManager</code>
 * responds with an {@link AllocateResponse} that informs the
 * <code>ApplicationMaster</code> of the set of newly allocated containers,
 * completed containers as well as current state of available resources.
 * </p>
 *
 * <p>
 * For each allocated container, the <code>ApplicationMaster</code> can then set
 * up the necessary launch context via {@link ContainerLaunchContext} to specify
 * the allocated container id, local resources required by the executable, the
 * environment to be setup for the executable, commands to execute, etc. and
 * submit a {@link StartContainerRequest} to the {@link ContainerManagementProtocol} to
 * launch and execute the defined commands on the given allocated container.
 * </p>
 *
 * <p>
 * The <code>ApplicationMaster</code> can monitor the launched container by
 * either querying the <code>ResourceManager</code> using
 * {@link ApplicationMasterProtocol#allocate} to get updates on completed containers or via
 * the {@link ContainerManagementProtocol} by querying for the status of the allocated
 * container's {@link ContainerId}.
 *
 * <p>
 * After the job has been completed, the <code>ApplicationMaster</code> has to
 * send a {@link FinishApplicationMasterRequest} to the
 * <code>ResourceManager</code> to inform it that the
 * <code>ApplicationMaster</code> has been completed.
 */
@InterfaceAudience.Public
@InterfaceStability.Unstable
public class ApplicationMaster {

  private static final Log LOG = LogFactory.getLog(ApplicationMaster.class);

  @VisibleForTesting
  @Private
  public static enum DSEntity {
    DS_APP_ATTEMPT, DS_CONTAINER
  }

  // Configuration
  private Configuration conf;

  // Handle to communicate with the Resource Manager
  private AMRMClientAsync<AMRMClient.ContainerRequest> amRMClient;

  // In both secure and non-secure modes, this points to the job-submitter.
  @VisibleForTesting
  UserGroupInformation appSubmitterUgi;

  // Handle to communicate with the Node Manager
  private NMClientAsync nmClientAsync;
  // Listen to process the response from the Node Manager

  // Application Attempt Id ( combination of attemptId and fail count )
  @VisibleForTesting
  protected ApplicationAttemptId appAttemptID;

  // TODO
  // For status update for clients - yet to be implemented
  // Hostname of the container
  private String appMasterHostname = "";
  // Port on which the app master listens for status updates from clients
  private int appMasterRpcPort = -1;
  // Tracking url to which app master publishes info for clients to monitor
  private String appMasterTrackingUrl = "";


  // Env variables to be setup for the shell command
  private Map<String, String> shellEnv = new HashMap<>();

  // Timeline domain ID
  private String domainId = null;

  // Hardcoded path to custom log_properties
  private static final String log4jPath = "log4j.properties";

  private ByteBuffer allTokens;
  private ContainerRequestListener listener;
  // Launch threads
  private ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
  private Queue<ModelRequest> requestQueue;
  // Timeline Client
  @VisibleForTesting
  TimelineClient timelineClient;

  private String zkQuorum;
  private String zkRoot;
  private MaaSHandler maasHandler;
  private Path appJarPath;

  public enum AMOptions {
    HELP("h", code -> {
      Option o = new Option(code, "help", false, "This screen");
      o.setRequired(false);
      return o;
    })
    ,ZK_QUORUM("zq", code -> {
      Option o = new Option(code, "zk_quorum", true, "Zookeeper Quorum");
      o.setRequired(true);
      return o;
    })
    ,ZK_ROOT("zr", code -> {
      Option o = new Option(code, "zk_root", true, "Zookeeper Root");
      o.setRequired(true);
      return o;
    })
    ,APP_JAR_PATH("aj", code -> {
      Option o = new Option(code, "app_jar_path", true, "App Jar Path");
      o.setRequired(true);
      return o;
    })
    ,APP_ATTEMPT_ID("aid", code -> {
      Option o = new Option(code, "app_attempt_id", true, "App Attempt ID. Not to be used unless for testing purposes");
      o.setRequired(false);
      return o;
    })
    ;
    Option option;
    String shortCode;
    AMOptions(String shortCode
                 , Function<String, Option> optionHandler
                 ) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }
    public String get(CommandLine cli, String def) {
      return has(cli)?cli.getOptionValue(shortCode):def;
    }

    public Map.Entry<AMOptions, String> of(String value) {
      if(option.hasArg()) {
        return new AbstractMap.SimpleEntry<>(this, value);
      }
      return new AbstractMap.SimpleEntry<>(this, null);
    }

    @SafeVarargs
    public static String toArgs(Map.Entry<AMOptions, String> ... arg) {
      return
      Joiner.on(" ").join(Iterables.transform(Arrays.asList(arg)
                                             , a -> "-" + a.getKey().shortCode
                                                  + (a.getValue() == null?"":(" " + a.getValue()))
                                             )
                         );

    }
    public static CommandLine parse(CommandLineParser parser, String[] args) throws ParseException {
      try {
        CommandLine cli = parser.parse(getOptions(), args);
        if(HELP.has(cli)) {
          printHelp();
          System.exit(0);
        }
        return cli;
      } catch (ParseException e) {
        System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
        e.printStackTrace(System.err);
        printHelp();
        throw e;
      }
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "MaaSApplicationMaster", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(AMOptions o : AMOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  /**
   * @param args Command line args
   */
  public static void main(String[] args) {
    boolean result = false;
    try {
      ApplicationMaster appMaster = new ApplicationMaster();
      LOG.info("Initializing ApplicationMaster");
      boolean doRun = appMaster.init(args);
      if (!doRun) {
        System.exit(0);
      }
      appMaster.run();
      result = appMaster.finish();
    } catch (Throwable t) {
      LOG.fatal("Error running ApplicationMaster", t);
      LogManager.shutdown();
      ExitUtil.terminate(1, t);
    }
    if (result) {
      LOG.info("Application Master completed successfully. exiting");
      System.exit(0);
    } else {
      LOG.info("Application Master failed. exiting");
      System.exit(2);
    }
  }


  public ApplicationMaster() {
    // Set up the configuration
    conf = new YarnConfiguration();
  }

  /**
   * Parse command line options
   *
   * @param args Command line args
   * @return Whether init successful and run should be invoked
   * @throws ParseException
   * @throws IOException
   */
  public boolean init(String[] args) throws ParseException, IOException {
    CommandLine cliParser = AMOptions.parse(new GnuParser(), args);

    //Check whether customer log4j.properties file exists
    if (fileExist(log4jPath)) {
      try {
        Log4jPropertyHelper.updateLog4jConfiguration(ApplicationMaster.class,
                log4jPath);
      } catch (Exception e) {
        LOG.warn("Can not set up custom log4j properties. " + e);
      }
    }

    if (AMOptions.HELP.has(cliParser)) {
      AMOptions.printHelp();
      return false;
    }


    zkQuorum = AMOptions.ZK_QUORUM.get(cliParser);
    zkRoot = AMOptions.ZK_ROOT.get(cliParser);
    appJarPath = new Path(AMOptions.APP_JAR_PATH.get(cliParser));

    Map<String, String> envs = System.getenv();

    if (!envs.containsKey(Environment.CONTAINER_ID.name())) {
      if (AMOptions.APP_ATTEMPT_ID.has(cliParser)) {
        String appIdStr = AMOptions.APP_ATTEMPT_ID.get(cliParser, "");
        appAttemptID = ConverterUtils.toApplicationAttemptId(appIdStr);
      } else {
        throw new IllegalArgumentException(
                "Application Attempt Id not set in the environment");
      }
    } else {
      ContainerId containerId = ConverterUtils.toContainerId(envs
              .get(Environment.CONTAINER_ID.name()));
      appAttemptID = containerId.getApplicationAttemptId();
    }

    if (!envs.containsKey(ApplicationConstants.APP_SUBMIT_TIME_ENV)) {
      throw new RuntimeException(ApplicationConstants.APP_SUBMIT_TIME_ENV
              + " not set in the environment");
    }
    if (!envs.containsKey(Environment.NM_HOST.name())) {
      throw new RuntimeException(Environment.NM_HOST.name()
              + " not set in the environment");
    }
    if (!envs.containsKey(Environment.NM_HTTP_PORT.name())) {
      throw new RuntimeException(Environment.NM_HTTP_PORT
              + " not set in the environment");
    }
    if (!envs.containsKey(Environment.NM_PORT.name())) {
      throw new RuntimeException(Environment.NM_PORT.name()
              + " not set in the environment");
    }

    LOG.info("Application master for app" + ", appId="
            + appAttemptID.getApplicationId().getId() + ", clustertimestamp="
            + appAttemptID.getApplicationId().getClusterTimestamp()
            + ", attemptId=" + appAttemptID.getAttemptId());


    if (cliParser.hasOption("shell_env")) {
      String shellEnvs[] = cliParser.getOptionValues("shell_env");
      for (String env : shellEnvs) {
        env = env.trim();
        int index = env.indexOf('=');
        if (index == -1) {
          shellEnv.put(env, "");
          continue;
        }
        String key = env.substring(0, index);
        String val = "";
        if (index < (env.length() - 1)) {
          val = env.substring(index + 1);
        }
        shellEnv.put(key, val);
      }
    }


    if (envs.containsKey(Constants.TIMELINEDOMAIN)) {
      domainId = envs.get(Constants.TIMELINEDOMAIN);
    }
    return true;
  }

  /**
   * Main run function for the application master
   *
   * @throws YarnException
   * @throws IOException
   */
  @SuppressWarnings({ "unchecked" })
  public void run() throws YarnException, IOException, InterruptedException {
    LOG.info("Starting ApplicationMaster");

    // Note: Credentials, Token, UserGroupInformation, DataOutputBuffer class
    // are marked as LimitedPrivate
    Credentials credentials =
            UserGroupInformation.getCurrentUser().getCredentials();

    allTokens = YarnUtils.INSTANCE.tokensFromCredentials(credentials);

    // Create appSubmitterUgi and add original tokens to it
    appSubmitterUgi = YarnUtils.INSTANCE.createUserGroup(credentials);
    startTimelineClient(conf);
    if(timelineClient != null) {
      YarnUtils.INSTANCE.publishApplicationAttemptEvent(timelineClient, appAttemptID.toString(),
              ContainerEvents.APP_ATTEMPT_START, domainId, appSubmitterUgi);
    }
    int minSize = getMinContainerMemoryIncrement(conf);
    listener = new ContainerRequestListener(timelineClient , appSubmitterUgi , domainId, minSize);
    amRMClient = AMRMClientAsync.createAMRMClientAsync(1000, listener);
    amRMClient.init(conf);
    amRMClient.start();

    nmClientAsync = new NMClientAsyncImpl(listener);
    nmClientAsync.init(conf);
    nmClientAsync.start();


    // Setup local RPC Server to accept status requests directly from clients
    // TODO need to setup a protocol for client to be able to communicate to
    // the RPC server
    // TODO use the rpc port info to register with the RM for the client to
    // send requests to this app master

    // Register self with ResourceManager
    // This will start heartbeating to the RM
    appMasterHostname = NetUtils.getHostname();
    RegisterApplicationMasterResponse response = amRMClient
            .registerApplicationMaster(appMasterHostname, appMasterRpcPort,
                    appMasterTrackingUrl);
    // Dump out information about cluster capability as seen by the
    // resource manager
    int maxMem = response.getMaximumResourceCapability().getMemory();
    LOG.info("Max mem capabililty of resources in this cluster " + maxMem);

    int maxVCores = response.getMaximumResourceCapability().getVirtualCores();
    LOG.info("Max vcores capabililty of resources in this cluster " + maxVCores);
    maasHandler = new MaaSHandler(zkQuorum, zkRoot);
    try {
      maasHandler.start();
      maasHandler.getDiscoverer().resetState();
      listener.initialize(amRMClient, nmClientAsync, maasHandler.getDiscoverer());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to find zookeeper", e);
    }
    EnumMap<Resources, Integer> maxResources = Resources.toResourceMap( Resources.MEMORY.of(maxMem)
                                                                      , Resources.V_CORE.of(maxVCores)
                                                                      );
    requestQueue = maasHandler.getConfig()
                              .createQueue(ImmutableMap.of(ZKQueue.ZK_CLIENT, maasHandler.getClient()
                                                          )
                                          );
    LOG.info("Ready to accept requests...");
    while(true) {

      ModelRequest request = requestQueue.dequeue();
      if(request == null) {
        LOG.error("Received a null request...");
        continue;
      }
      LOG.info("[" + request.getAction() + "]: Received request for model " + request.getName() + ":" + request.getVersion() + "x" + request.getNumInstances()
              + " containers of size " + request.getMemory() + "M at path " + request.getPath()
              );
      EnumMap<Resources, Integer> resourceRequest = Resources.toResourceMap(Resources.MEMORY.of(request.getMemory())
                                                                            ,Resources.V_CORE.of(1)
                                                                            );
      EnumMap<Resources, Integer> resources = Resources.getRealisticResourceRequest( maxResources
                                                                                   , Resources.toResource(resourceRequest)
                                                                                   );
      Resource resource = Resources.toResource(resources);
      Path appMasterJar  = getAppMasterJar();
      if(request.getAction() == Action.ADD) {
        listener.requestContainers(request.getNumInstances(), resource);
        for (int i = 0; i < request.getNumInstances(); ++i) {
          Container container = listener.getContainers(resource).take();
          LOG.info("Found container id of " + container.getId().getContainerId());
          executor.execute(new LaunchContainer(conf
                        , zkQuorum
                        , zkRoot
                        , nmClientAsync
                        , request
                        , container
                        , allTokens
                        , appMasterJar
                                              )
                          );
          listener.getContainerState().registerRequest(container, request);
        }
      }
      else if(request.getAction() == Action.REMOVE) {
        listener.removeContainers(request.getNumInstances(), request);
      }
    }
  }

  private Path getAppMasterJar() {
    return appJarPath;
  }
  private int getMinContainerMemoryIncrement(Configuration conf) {
    String incrementStr = conf.get("yarn.scheduler.increment-allocation-mb");
    if(incrementStr == null || incrementStr.length() == 0) {
      incrementStr = conf.get("yarn.scheduler.minimum-allocation-mb");
    }
    return Integer.parseInt(incrementStr);
  }

  @VisibleForTesting
  void startTimelineClient(final Configuration conf)
          throws YarnException, IOException, InterruptedException {
    try {
      appSubmitterUgi.doAs(new PrivilegedExceptionAction<Void>() {
        @Override
        public Void run() throws Exception {
          if (conf.getBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED,
                  YarnConfiguration.DEFAULT_TIMELINE_SERVICE_ENABLED)) {
            // Creating the Timeline Client
            timelineClient = TimelineClient.createTimelineClient();
            timelineClient.init(conf);
            timelineClient.start();
          } else {
            timelineClient = null;
            LOG.warn("Timeline service is not enabled");
          }
          return null;
        }
      });
    } catch (UndeclaredThrowableException e) {
      throw new YarnException(e.getCause());
    }
  }


  @VisibleForTesting
  protected boolean finish() {
    return true;
  }

  private boolean fileExist(String filePath) {
    return new File(filePath).exists();
  }

}
