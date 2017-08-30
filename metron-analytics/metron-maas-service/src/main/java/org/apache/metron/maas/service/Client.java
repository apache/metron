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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.protocolrecords.KillApplicationRequest;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.QueueACL;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.QueueUserACLInfo;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.api.records.YarnClusterMetrics;
import org.apache.hadoop.yarn.api.records.timeline.TimelineDomain;
import org.apache.hadoop.yarn.client.api.TimelineClient;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;

import static org.apache.metron.maas.service.Client.ClientOptions.*;


/**
 * Client for Distributed Shell application submission to YARN.
 *
 * <p> The distributed shell client allows an application master to be launched that in turn would run
 * the provided shell command on a set of containers. </p>
 *
 * <p>This client is meant to act as an example on how to write yarn-based applications. </p>
 *
 * <p> To submit an application, a client first needs to connect to the <code>ResourceManager</code>
 * aka ApplicationsManager or ASM via the {@link ApplicationClientProtocol}. The {@link ApplicationClientProtocol}
 * provides a way for the client to get access to cluster information and to request for a
 * new {@link ApplicationId}. <p>
 *
 * <p> For the actual job submission, the client first has to create an {@link ApplicationSubmissionContext}.
 * The {@link ApplicationSubmissionContext} defines the application details such as {@link ApplicationId}
 * and application name, the priority assigned to the application and the queue
 * to which this application needs to be assigned. In addition to this, the {@link ApplicationSubmissionContext}
 * also defines the {@link ContainerLaunchContext} which describes the <code>Container</code> with which
 * the {@link ApplicationMaster} is launched. </p>
 *
 * <p> The {@link ContainerLaunchContext} in this scenario defines the resources to be allocated for the
 * {@link ApplicationMaster}'s container, the local resources (jars, configuration files) to be made available
 * and the environment to be set for the {@link ApplicationMaster} and the commands to be executed to run the
 * {@link ApplicationMaster}. <p>
 *
 * <p> Using the {@link ApplicationSubmissionContext}, the client submits the application to the
 * <code>ResourceManager</code> and then monitors the application by requesting the <code>ResourceManager</code>
 * for an {@link ApplicationReport} at regular time intervals. In case of the application taking too long, the client
 * kills the application by submitting a {@link KillApplicationRequest} to the <code>ResourceManager</code>. </p>
 *
 */
@InterfaceAudience.Public
@InterfaceStability.Unstable
public class Client {

  private static final Log LOG = LogFactory.getLog(Client.class);

  // Configuration
  private Configuration conf;
  private YarnClient yarnClient;
  // Application master specific info to register a new Application with RM/ASM
  private String appName = "";
  // App master priority
  private int amPriority = 0;
  // Queue for App master
  private String amQueue = "";
  // Amt. of memory resource to request for to run the App Master
  private int amMemory = 10;
  // Amt. of virtual core resource to request for to run the App Master
  private int amVCores = 1;

  // Application master jar file
  private String appMasterJar = "";
  // Main class to invoke application master
  private final String appMasterMainClass;

  // Env variables to be setup for the shell command
  private Map<String, String> shellEnv = new HashMap<String, String>();
  private String nodeLabelExpression = null;

  // log4j.properties file
  // if available, add to local resources and set into classpath
  private String log4jPropFile = "";

  // Start time for client
  private final long clientStartTime = System.currentTimeMillis();
  // Timeout threshold for client. Kill app after time interval expires.
  private long clientTimeout = 600000;

  // flag to indicate whether to keep containers across application attempts.
  private boolean keepContainers = false;

  private long attemptFailuresValidityInterval = -1;

  // Timeline domain ID
  private String domainId = null;
  private String zkQuorum = null;
  private String zkRoot = null;
  // Flag to indicate whether to create the domain of the given ID
  private boolean toCreateDomain = false;

  // Timeline domain reader access control
  private String viewACLs = null;

  // Timeline domain writer access control
  private String modifyACLs = null;


  private static final String appMasterJarPath = "AppMaster.jar";
  // Hardcoded path to custom log_properties
  private static final String log4jPath = "log4j.properties";

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    boolean result = false;
    try {
      Client client = new Client();
      LOG.info("Initializing Client");
      try {
        boolean doRun = client.init(args);
        if (!doRun) {
          System.exit(0);
        }
      } catch (IllegalArgumentException e) {
        System.err.println(e.getLocalizedMessage());
        System.exit(-1);
      }
      result = client.run();
    } catch (Throwable t) {
      LOG.fatal("Error running Client", t);
      System.exit(1);
    }
    if (result) {
      LOG.info("Application completed successfully");
      System.exit(0);
    }
    LOG.error("Application failed to complete successfully");
    System.exit(2);
  }

  /**
   */
  public Client(Configuration conf) throws Exception  {
    this(
            ApplicationMaster.class.getName(),
            conf);
  }

  public enum ClientOptions {
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
      o.setRequired(false);
      return o;
    })
    ,SHELL_ENV("e", code -> {
      Option o = new Option(code, "shell_env", true,
              "Environment for shell script. Specified as env_key=env_val pairs");
      o.setRequired(false);
      return o;
    })
    ,QUEUE("q", code -> {
      Option o = new Option(code, "queue", true,"RM Queue in which this application is to be submitted");
      o.setRequired(false);
      return o;
    })
    ,TIMEOUT("t", code -> {
      Option o = new Option(code, "timeout", true,"Application timeout in milliseconds");
      o.setRequired(false);
      return o;
    })
    ,MASTER_MEMORY("mm", code -> {
      Option o = new Option(code, "master_memory", true,"Amount of memory in MB to be requested to run the application master");
      o.setRequired(false);
      return o;
    })
    ,MASTER_VCORE("mc", code -> {
      Option o = new Option(code, "master_vcores", true,"Amount of virtual cores to be requested to run the application master");
      o.setRequired(false);
      return o;
    })
    ,JAR("j", code -> {
      Option o = new Option(code, "jar", true,"Jar file containing the application master");
      o.setRequired(false);
      return o;
    })
    ,LOG4J_PROPERTIES("l", code -> {
      Option o = new Option(code, "log4j", true, "The log4j properties file to load");
      o.setRequired(false);
      return o;
    })
    ,DOMAIN("d", code -> {
      Option o = new Option(code, "domain", true,"ID of the timeline domain where the timeline entities will be put");
      o.setRequired(false);
      return o;
    })
    ,VIEW_ACLS("va", code -> {
      Option o = new Option(code, "view_acls", true,"Users and groups that allowed to view the timeline entities in the given domain");
      o.setRequired(false);
      return o;
    })
    ,MODIFY_ACLS("ma", code -> {
      Option o = new Option(code, "modify_acls", true,"Users and groups that allowed to modify the timeline entities in the given domain");
      o.setRequired(false);
      return o;
    })
    ,CREATE("c", code -> {
      Option o = new Option(code, "create", false,"Flag to indicate whether to create the domain specified with -domain.");
      o.setRequired(false);
      return o;
    })
    ,NODE_LABEL_EXPRESSION("nle", code -> {
      Option o = new Option(code, "node_label_expression", true,
            "Node label expression to determine the nodes"
                    + " where all the containers of this application"
                    + " will be allocated, \"\" means containers"
                    + " can be allocated anywhere, if you don't specify the option,"
                    + " default node_label_expression of queue will be used.");
      o.setRequired(false);
      return o;
    })
    ;
    Option option;
    String shortCode;
    ClientOptions(String shortCode
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

    public Map.Entry<ClientOptions, String> of(String value) {
      if(option.hasArg()) {
        return new AbstractMap.SimpleEntry<>(this, value);
      }
      return new AbstractMap.SimpleEntry<>(this, null);
    }

    @SafeVarargs
    public static String toArgs(Map.Entry<ClientOptions, String> ... arg) {
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
      formatter.printHelp( "MaaSClient", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(ClientOptions o : ClientOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }
  Client(String appMasterMainClass, Configuration conf) {
    this.conf = conf;
    this.appMasterMainClass = appMasterMainClass;
    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(conf);
  }

  /**
   */
  public Client() throws Exception  {
    this(new YarnConfiguration());
  }
  public static String getJar(Class klass) throws URISyntaxException {
    return klass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
  }
  /**
   * Parse command line options
   * @param args Parsed command line options
   * @return Whether the init was successful to run the client
   * @throws ParseException
   */
  public boolean init(String[] args) throws ParseException {

    CommandLine cli =ClientOptions.parse(new PosixParser(), args);

    if (LOG4J_PROPERTIES.has(cli)) {
      String log4jPath = LOG4J_PROPERTIES.get(cli);
      try {
        Log4jPropertyHelper.updateLog4jConfiguration(Client.class, log4jPath);
      } catch (Exception e) {
        LOG.warn("Can not set up custom log4j properties. " + e);
      }
    }


    keepContainers = false;
    zkQuorum = ZK_QUORUM.get(cli);
    zkRoot = ZK_ROOT.get(cli, "/metron/maas/config");
    appName = "MaaS";
    amPriority = 0;
    amQueue = QUEUE.get(cli, "default");
    amMemory = Integer.parseInt(MASTER_MEMORY.get(cli, "10"));
    amVCores = Integer.parseInt(MASTER_VCORE.get(cli, "1"));

    if (amMemory < 0) {
      throw new IllegalArgumentException("Invalid memory specified for application master, exiting."
              + " Specified memory=" + amMemory);
    }
    if (amVCores < 0) {
      throw new IllegalArgumentException("Invalid virtual cores specified for application master, exiting."
              + " Specified virtual cores=" + amVCores);
    }

    if (!JAR.has(cli)){
      try {
        appMasterJar = getJar(ApplicationMaster.class);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("No jar file specified for application master: " + e.getMessage(), e);
      }
    }
    else {
      appMasterJar = JAR.get(cli);
    }

    if (SHELL_ENV.has(cli)) {
      String envs[] = cli.getOptionValues(SHELL_ENV.option.getOpt());
      for (String env : envs) {
        env = env.trim();
        int index = env.indexOf('=');
        if (index == -1) {
          shellEnv.put(env, "");
          continue;
        }
        String key = env.substring(0, index);
        String val = "";
        if (index < (env.length()-1)) {
          val = env.substring(index+1);
        }
        shellEnv.put(key, val);
      }
    }


    nodeLabelExpression = NODE_LABEL_EXPRESSION.get(cli, null);

    clientTimeout = Integer.parseInt(TIMEOUT.get(cli, "600000"));

    attemptFailuresValidityInterval = -1;

    log4jPropFile = LOG4J_PROPERTIES.get(cli, "");

    // Get timeline domain options
    if (DOMAIN.has(cli)) {
      domainId = DOMAIN.get(cli);
      toCreateDomain = CREATE.has(cli);
      if (VIEW_ACLS.has(cli)) {
        viewACLs = VIEW_ACLS.get(cli);
      }
      if (MODIFY_ACLS.has(cli)) {
        modifyACLs = MODIFY_ACLS.get(cli);
      }
    }
    return true;
  }

  /**
   * Main run function for the client
   * @return true if application completed successfully
   * @throws IOException
   * @throws YarnException
   */
  public boolean run() throws IOException, YarnException {

    LOG.info("Running Client");
    yarnClient.start();

    YarnClusterMetrics clusterMetrics = yarnClient.getYarnClusterMetrics();
    LOG.info("Got Cluster metric info from ASM"
            + ", numNodeManagers=" + clusterMetrics.getNumNodeManagers());

    List<NodeReport> clusterNodeReports = yarnClient.getNodeReports(
            NodeState.RUNNING);
    LOG.info("Got Cluster node info from ASM");
    for (NodeReport node : clusterNodeReports) {
      LOG.info("Got node report from ASM for"
              + ", nodeId=" + node.getNodeId()
              + ", nodeAddress" + node.getHttpAddress()
              + ", nodeRackName" + node.getRackName()
              + ", nodeNumContainers" + node.getNumContainers());
    }

    QueueInfo queueInfo = yarnClient.getQueueInfo(this.amQueue);
    LOG.info("Queue info"
            + ", queueName=" + queueInfo.getQueueName()
            + ", queueCurrentCapacity=" + queueInfo.getCurrentCapacity()
            + ", queueMaxCapacity=" + queueInfo.getMaximumCapacity()
            + ", queueApplicationCount=" + queueInfo.getApplications().size()
            + ", queueChildQueueCount=" + queueInfo.getChildQueues().size());

    List<QueueUserACLInfo> listAclInfo = yarnClient.getQueueAclsInfo();
    for (QueueUserACLInfo aclInfo : listAclInfo) {
      for (QueueACL userAcl : aclInfo.getUserAcls()) {
        LOG.info("User ACL Info for Queue"
                + ", queueName=" + aclInfo.getQueueName()
                + ", userAcl=" + userAcl.name());
      }
    }

    if (domainId != null && domainId.length() > 0 && toCreateDomain) {
      prepareTimelineDomain();
    }

    // Get a new application id
    YarnClientApplication app = yarnClient.createApplication();
    GetNewApplicationResponse appResponse = app.getNewApplicationResponse();
    // TODO get min/max resource capabilities from RM and change memory ask if needed
    // If we do not have min/max, we may not be able to correctly request
    // the required resources from the RM for the app master
    // Memory ask has to be a multiple of min and less than max.
    // Dump out information about cluster capability as seen by the resource manager
    int maxMem = appResponse.getMaximumResourceCapability().getMemory();
    LOG.info("Max mem capabililty of resources in this cluster " + maxMem);

    // A resource ask cannot exceed the max.
    if (amMemory > maxMem) {
      LOG.info("AM memory specified above max threshold of cluster. Using max value."
              + ", specified=" + amMemory
              + ", max=" + maxMem);
      amMemory = maxMem;
    }

    int maxVCores = appResponse.getMaximumResourceCapability().getVirtualCores();
    LOG.info("Max virtual cores capabililty of resources in this cluster " + maxVCores);

    if (amVCores > maxVCores) {
      LOG.info("AM virtual cores specified above max threshold of cluster. "
              + "Using max value." + ", specified=" + amVCores
              + ", max=" + maxVCores);
      amVCores = maxVCores;
    }

    // set the application name
    ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
    ApplicationId appId = appContext.getApplicationId();

    appContext.setKeepContainersAcrossApplicationAttempts(keepContainers);
    appContext.setApplicationName(appName);

    if (attemptFailuresValidityInterval >= 0) {
      appContext
              .setAttemptFailuresValidityInterval(attemptFailuresValidityInterval);
    }

    // set local resources for the application master
    // local files or archives as needed
    // In this scenario, the jar file for the application master is part of the local resources
    Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();

    LOG.info("Copy App Master jar from local filesystem and add to local environment");
    // Copy the application master jar to the filesystem
    // Create a local resource to point to the destination jar path
    FileSystem fs = FileSystem.get(conf);
    createMaaSDirectory(fs, appId.toString());
    Path ajPath = addToLocalResources(fs, appMasterJar, appMasterJarPath, appId.toString(), localResources, null);

    // Set the log4j properties if needed
    if (!log4jPropFile.isEmpty()) {
      addToLocalResources(fs, log4jPropFile, log4jPath, appId.toString(),
              localResources, null);
    }


    // Set the necessary security tokens as needed
    //amContainer.setContainerTokens(containerToken);

    // Set the env variables to be setup in the env where the application master will be run
    LOG.info("Set the environment for the application master");
    Map<String, String> env = new HashMap<String, String>();

    // put location of shell script into env
    // using the env info, the application master will create the correct local resource for the
    // eventual containers that will be launched to execute the shell scripts
    if (domainId != null && domainId.length() > 0) {
      env.put(Constants.TIMELINEDOMAIN, domainId);
    }

    // Add AppMaster.jar location to classpath
    // At some point we should not be required to add
    // the hadoop specific classpaths to the env.
    // It should be provided out of the box.
    // For now setting all required classpaths including
    // the classpath to "." for the application jar
    StringBuilder classPathEnv = new StringBuilder(Environment.CLASSPATH.$$())
            .append(ApplicationConstants.CLASS_PATH_SEPARATOR).append("./*");
    for (String c : conf.getStrings(
            YarnConfiguration.YARN_APPLICATION_CLASSPATH,
            YarnConfiguration.DEFAULT_YARN_CROSS_PLATFORM_APPLICATION_CLASSPATH)) {
      classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR);
      classPathEnv.append(c.trim());
    }
    classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR).append(
            "./log4j.properties");

    // add the runtime classpath needed for tests to work
    if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
      classPathEnv.append(':');
      classPathEnv.append(System.getProperty("java.class.path"));
    }

    env.put("CLASSPATH", classPathEnv.toString());

    // Set the necessary command to execute the application master
    Vector<CharSequence> vargs = new Vector<CharSequence>(30);

    // Set java executable command
    LOG.info("Setting up app master command");
    vargs.add(Environment.JAVA_HOME.$$() + "/bin/java");
    // Set Xmx based on am memory size
    vargs.add("-Xmx" + amMemory + "m");
    // Set class name
    vargs.add(appMasterMainClass);
    // Set params for Application Master
    vargs.add(ApplicationMaster.AMOptions.toArgs(ApplicationMaster.AMOptions.ZK_QUORUM.of(zkQuorum)
                                                ,ApplicationMaster.AMOptions.ZK_ROOT.of(zkRoot)
                                                ,ApplicationMaster.AMOptions.APP_JAR_PATH.of(ajPath.toString())
                                                )
             );
    if (null != nodeLabelExpression) {
      appContext.setNodeLabelExpression(nodeLabelExpression);
    }
    for (Map.Entry<String, String> entry : shellEnv.entrySet()) {
      vargs.add("--shell_env " + entry.getKey() + "=" + entry.getValue());
    }

    vargs.add("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout");
    vargs.add("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr");

    // Get final commmand
    StringBuilder command = new StringBuilder();
    for (CharSequence str : vargs) {
      command.append(str).append(" ");
    }

    LOG.info("Completed setting up app master command " + command.toString());
    List<String> commands = new ArrayList<String>();
    commands.add(command.toString());

    // Set up the container launch context for the application master
    ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
            localResources, env, commands, null, null, null);

    // Set up resource type requirements
    // For now, both memory and vcores are supported, so we set memory and
    // vcores requirements
    Resource capability = Resource.newInstance(amMemory, amVCores);
    appContext.setResource(capability);

    // Service data is a binary blob that can be passed to the application
    // Not needed in this scenario

    // Setup security tokens
    if (UserGroupInformation.isSecurityEnabled()) {
      // Note: Credentials class is marked as LimitedPrivate for HDFS and MapReduce
      Credentials credentials = new Credentials();
      String tokenRenewer = conf.get(YarnConfiguration.RM_PRINCIPAL);
      if (tokenRenewer == null || tokenRenewer.length() == 0) {
        throw new IOException(
                "Can't get Master Kerberos principal for the RM to use as renewer");
      }

      // For now, only getting tokens for the default file-system.
      final Token<?> tokens[] =
              fs.addDelegationTokens(tokenRenewer, credentials);
      if (tokens != null) {
        for (Token<?> token : tokens) {
          LOG.info("Got dt for " + fs.getUri() + "; " + token);
        }
      }
      DataOutputBuffer dob = new DataOutputBuffer();
      credentials.writeTokenStorageToStream(dob);
      ByteBuffer fsTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
      amContainer.setTokens(fsTokens);
    }

    appContext.setAMContainerSpec(amContainer);

    // Set the priority for the application master
    Priority pri = Priority.newInstance(amPriority);
    appContext.setPriority(pri);

    // Set the queue to which this application is to be submitted in the RM
    appContext.setQueue(amQueue);

    // Submit the application to the applications manager
    // SubmitApplicationResponse submitResp = applicationsManager.submitApplication(appRequest);
    // Ignore the response as either a valid response object is returned on success
    // or an exception thrown to denote some form of a failure
    LOG.info("Submitting application to ASM");

    yarnClient.submitApplication(appContext);

    // Monitor the application
    return monitorApplication(appId);

  }

  /**
   * Monitor the submitted application for completion.
   * Kill application if time expires.
   * @param appId Application Id of application to be monitored
   * @return true if application completed successfully
   * @throws YarnException
   * @throws IOException
   */
  private boolean monitorApplication(ApplicationId appId)
          throws YarnException, IOException {

    while (true) {

      // Check app status every 1 second.
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOG.debug("Thread sleep in monitoring loop interrupted");
      }

      // Get application report for the appId we are interested in
      ApplicationReport report = yarnClient.getApplicationReport(appId);

      LOG.info("Got application report from ASM for"
              + ", appId=" + appId.getId()
              + ", clientToAMToken=" + report.getClientToAMToken()
              + ", appDiagnostics=" + report.getDiagnostics()
              + ", appMasterHost=" + report.getHost()
              + ", appQueue=" + report.getQueue()
              + ", appMasterRpcPort=" + report.getRpcPort()
              + ", appStartTime=" + report.getStartTime()
              + ", yarnAppState=" + report.getYarnApplicationState().toString()
              + ", distributedFinalState=" + report.getFinalApplicationStatus().toString()
              + ", appTrackingUrl=" + report.getTrackingUrl()
              + ", appUser=" + report.getUser());

      YarnApplicationState state = report.getYarnApplicationState();
      FinalApplicationStatus dsStatus = report.getFinalApplicationStatus();
      if(YarnApplicationState.RUNNING == state) {
        LOG.info("Application is running...");
        return true;
      }
      if (YarnApplicationState.FINISHED == state) {
        if (FinalApplicationStatus.SUCCEEDED == dsStatus) {
          LOG.info("Application has completed successfully. Breaking monitoring loop");
          return true;
        }
        else {
          LOG.info("Application did finished unsuccessfully."
                  + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                  + ". Breaking monitoring loop");
          return false;
        }
      }
      else if (YarnApplicationState.KILLED == state
              || YarnApplicationState.FAILED == state) {
        LOG.info("Application did not finish."
                + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                + ". Breaking monitoring loop");
        return false;
      }

      if (System.currentTimeMillis() > (clientStartTime + clientTimeout)) {
        LOG.info("Reached client specified timeout for application. Killing application");
        forceKillApplication(appId);
        return false;
      }
    }

  }

  /**
   * Kill a submitted application by sending a call to the ASM
   * @param appId Application Id to be killed.
   * @throws YarnException
   * @throws IOException
   */
  private void forceKillApplication(ApplicationId appId)
          throws YarnException, IOException {
    // TODO clarify whether multiple jobs with the same app id can be submitted and be running at
    // the same time.
    // If yes, can we kill a particular attempt only?

    // Response can be ignored as it is non-null on success or
    // throws an exception in case of failures
    yarnClient.killApplication(appId);
  }

  private void createMaaSDirectory(FileSystem fs, String appId) throws IOException {
    for(Path p : ImmutableList.of(new Path(fs.getHomeDirectory(), appName)
                                 , new Path(fs.getHomeDirectory(), appName + "/" + appId)
                                 )
       ) {
      if(!fs.exists(p)) {
        fs.mkdirs(p);
        fs.setPermission(p, new FsPermission((short)0755));
      }
    }
  }

  private Path addToLocalResources(FileSystem fs, String fileSrcPath,
                                   String fileDstPath, String appId, Map<String, LocalResource> localResources,
                                   String resources) throws IOException {
    String suffix =
            appName + "/" + appId + "/" + fileDstPath;
    Path dst =
            new Path(fs.getHomeDirectory(), suffix);
    if (fileSrcPath == null) {
      FSDataOutputStream ostream = null;
      try {
        ostream = FileSystem
                .create(fs, dst, new FsPermission((short) 0710));
        ostream.writeUTF(resources);
      } finally {
        IOUtils.closeQuietly(ostream);
      }
    } else {
      fs.copyFromLocalFile(new Path(fileSrcPath), dst);
    }
    fs.setPermission(dst, new FsPermission((short)0755));
    FileStatus scFileStatus = fs.getFileStatus(dst);
    LocalResource scRsrc =
            LocalResource.newInstance(
                    ConverterUtils.getYarnUrlFromURI(dst.toUri()),
                    LocalResourceType.FILE, LocalResourceVisibility.APPLICATION,
                    scFileStatus.getLen(), scFileStatus.getModificationTime());
    localResources.put(fileDstPath, scRsrc);
    return dst;
  }

  private void prepareTimelineDomain() {
    TimelineClient timelineClient = null;
    if (conf.getBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED,
            YarnConfiguration.DEFAULT_TIMELINE_SERVICE_ENABLED)) {
      timelineClient = TimelineClient.createTimelineClient();
      timelineClient.init(conf);
      timelineClient.start();
    } else {
      LOG.warn("Cannot put the domain " + domainId +
              " because the timeline service is not enabled");
      return;
    }
    try {
      TimelineDomain domain = new TimelineDomain();
      domain.setId(domainId);
      domain.setReaders(
              viewACLs != null && viewACLs.length() > 0 ? viewACLs : " ");
      domain.setWriters(
              modifyACLs != null && modifyACLs.length() > 0 ? modifyACLs : " ");
      timelineClient.putDomain(domain);
      LOG.info("Put the timeline domain: " +
              TimelineUtils.dumpTimelineRecordtoJSON(domain));
    } catch (Exception e) {
      LOG.error("Error when putting the timeline domain", e);
    } finally {
      timelineClient.stop();
    }
  }
}
