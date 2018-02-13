/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.common.shell;

import com.google.common.collect.Maps;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.configuration.ConfigurationsUtils;
import org.apache.metron.stellar.common.shell.StellarExecutionListeners.FunctionDefinedListener;
import org.apache.metron.stellar.common.shell.StellarExecutionListeners.SpecialDefinedListener;
import org.apache.metron.stellar.common.shell.StellarExecutionListeners.VariableDefinedListener;
import org.apache.metron.stellar.common.shell.specials.AssignmentCommand;
import org.apache.metron.stellar.common.shell.specials.Comment;
import org.apache.metron.stellar.common.shell.specials.DocCommand;
import org.apache.metron.stellar.common.shell.specials.MagicDefineGlobal;
import org.apache.metron.stellar.common.shell.specials.MagicListFunctions;
import org.apache.metron.stellar.common.shell.specials.MagicListGlobals;
import org.apache.metron.stellar.common.shell.specials.MagicListVariables;
import org.apache.metron.stellar.common.shell.specials.MagicUndefineGlobal;
import org.apache.metron.stellar.common.shell.specials.QuitCommand;
import org.apache.metron.stellar.common.shell.specials.SpecialCommand;
import org.apache.metron.stellar.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctionInfo;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.apache.metron.stellar.common.configuration.ConfigurationsUtils.readGlobalConfigBytesFromZookeeper;
import static org.apache.metron.stellar.common.shell.StellarResult.noop;
import static org.apache.metron.stellar.common.shell.StellarResult.error;
import static org.apache.metron.stellar.common.shell.StellarResult.success;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;
import static org.apache.metron.stellar.dsl.Context.Capabilities.STELLAR_CONFIG;
import static org.apache.metron.stellar.dsl.Context.Capabilities.ZOOKEEPER_CLIENT;

/**
 * Default implementation of a StellarShellExecutor.
 */
public class DefaultStellarShellExecutor implements StellarShellExecutor {

  private static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String SHELL_VARIABLES = "shellVariables";

  /**
   * The variables known by Stellar.
   */
  private Map<String, VariableResult> variables;

  /**
   * The function resolver.
   */
  private FunctionResolver functionResolver;

  /**
   * A Zookeeper client. Only defined if given a valid Zookeeper URL.
   */
  private Optional<CuratorFramework> zkClient;

  /**
   * A registry of all special commands; like %magic, ?doc, and quit.
   */
  private List<SpecialCommand> specials;

  /**
   * The Stellar execution context.
   */
  private Context context;

  /**
   * Listeners that are notified when a function is defined.
   */
  private List<FunctionDefinedListener> functionListeners;

  /**
   * Listeners that are notified when a variable is defined.
   */
  private List<VariableDefinedListener> variableListeners;

  /**
   * Listeners that are notified when a special command is defined.
   */
  private List<SpecialDefinedListener> specialListeners;


  public DefaultStellarShellExecutor(
          FunctionResolver functionResolver,
          Properties properties,
          Optional<String> zookeeperUrl,
          List<SpecialCommand> specials) throws Exception {

    this.functionListeners = new ArrayList<>();
    this.variableListeners = new ArrayList<>();
    this.specialListeners = new ArrayList<>();
    this.variables = new HashMap<>();
    this.zkClient = createZookeeperClient(zookeeperUrl);
    this.context = createContext(properties, this.zkClient);
    this.functionResolver = functionResolver;
    this.specials = specials;
  }

  public DefaultStellarShellExecutor(
          FunctionResolver functionResolver,
          Properties properties,
          Optional<String> zookeeperUrl) throws Exception {

    this(functionResolver, properties, zookeeperUrl, defaultSpecials());
  }

  public DefaultStellarShellExecutor(
          Properties properties,
          Optional<String> zookeeperUrl) throws Exception {

    this(StellarFunctions.FUNCTION_RESOLVER(), properties, zookeeperUrl);
  }

  /**
   * The default specials that will be made available, if none are specified otherwise.
   * @return The default special commands.
   */
  public static List<SpecialCommand> defaultSpecials() {
    return Arrays.asList(
            new AssignmentCommand(),
            new DocCommand(),
            new QuitCommand(),
            new Comment(),
            new MagicListFunctions(),
            new MagicListVariables(),
            new MagicDefineGlobal(),
            new MagicUndefineGlobal(),
            new MagicListGlobals()
    );
  }

  @Override
  public void init() {
    StellarFunctions.initialize(this.context);

    // notify listeners about the available specials
    for(SpecialCommand command : specials) {
      notifySpecialListeners(command);
    }

    // notify listeners about the available functions
    for(StellarFunctionInfo fn : functionResolver.getFunctionInfo()) {
      notifyFunctionListeners(fn);
    }
  }

  /**
   * Add a listener that will be notified when a function is defined.
   * @param listener The listener to notify.
   */
  @Override
  public void addFunctionListener(FunctionDefinedListener listener) {
    this.functionListeners.add(listener);
  }

  /**
   * Notify function listeners that a function has been defined.
   * @param functionInfo The function that was defined.
   */
  private void notifyFunctionListeners(StellarFunctionInfo functionInfo) {
    for(FunctionDefinedListener listener : functionListeners) {
      listener.whenFunctionDefined(functionInfo);
    }
  }

  /**
   * Add a listener that will be notified when a variable is defined.
   * @param listener The listener to notify.
   */
  @Override
  public void addVariableListener(VariableDefinedListener listener) {
    this.variableListeners.add(listener);
  }

  /**
   * Notify variable listeners that a variable has been (re)defined.
   * @param variableName The variable name.
   * @param result The variable result.
   */
  private void notifyVariableListeners(String variableName, VariableResult result) {
    for(VariableDefinedListener listener : variableListeners) {
      listener.whenVariableDefined(variableName, result);
    }
  }

  /**
   * Add a listener that will be notified when a magic command is defined.
   * @param listener The listener to notify.
   */
  @Override
  public void addSpecialListener(SpecialDefinedListener listener) {
    this.specialListeners.add(listener);
  }

  /**
   * Notify listeners that a magic command has been defined.
   * @param specialCommand The magic command.
   */
  private void notifySpecialListeners(SpecialCommand specialCommand) {
    for(SpecialDefinedListener listener : specialListeners) {
      listener.whenSpecialDefined(specialCommand);
    }
  }

  @Override
  public StellarResult execute(String expression) {

    // if only whitespace, there is nothing to do
    expression = StringUtils.trimToEmpty(expression);
    if(StringUtils.isBlank(expression)) {
      return noop();
    }

    // is this a special command?
    for(SpecialCommand command : specials) {
      if(command.getMatcher().apply(expression)) {
        return command.execute(expression, this);
      }
    }

    // otherwise, this must be a stellar expression
    return executeStellar(expression);
  }

  /**
   * Retrieves the GLOBAL_CONFIG, if it exists.  If it does not, it creates the GLOBAL_CONFIG
   * and adds it to the Stellar execution context.
   *
   * @return The global configuration.
   */
  @Override
  public Map<String, Object> getGlobalConfig() {
    Map<String, Object> globals;
    Optional<Object> capability = getContext().getCapability(GLOBAL_CONFIG, false);
    if (capability.isPresent()) {
      globals = (Map<String, Object>) capability.get();

    } else {
      // if it does not exist, create it.  this creates the global config for the current stellar executor
      // session only.  this does not change the global config maintained externally in zookeeper
      globals = new HashMap<>();
      getContext().addCapability(GLOBAL_CONFIG, () -> globals);
    }

    return globals;
  }

  @Override
  public void assign(String variableName, Object value, Optional<String> expression) {

    // perform the variable assignment
    VariableResult varResult = VariableResult.withExpression(value, expression);
    this.variables.put(variableName, varResult);

    // notify any listeners
    notifyVariableListeners(variableName, varResult);
  }

  @Override
  public FunctionResolver getFunctionResolver() {
    return functionResolver;
  }

  @Override
  public Map<String, VariableResult> getState() {
    return UnmodifiableMap.decorate(variables);
  }

  /**
   * Returns all variables that have been defined.  Unlike 'getState' this unwraps
   * the VariableResult so that we have the actual value.
   *
   * @return All variables that have been defined.
   */
  public Map<String, Object> getVariables() {
    return Maps.transformValues(variables, (v) -> v.getResult());
  }

  @Override
  public Context getContext() {
    return context;
  }

  /**
   * Creates a Zookeeper client.
   * @param zookeeperUrl The Zookeeper URL.
   */
  private Optional<CuratorFramework> createZookeeperClient(Optional<String> zookeeperUrl) {
    Optional<CuratorFramework> client = Optional.empty();

    // can only create client, if have valid zookeeper URL
    if(zookeeperUrl.isPresent()) {
      String url = zookeeperUrl.get();
      if (StringUtils.isNotBlank(url)) {

        LOG.debug(String.format("Connecting to Zookeeper; url=%s", url));
        CuratorFramework c = ConfigurationsUtils.getClient(url);
        c.start();
        client = Optional.of(c);
      }
    }

    return client;
  }

  /**
   * Creates a Context initialized with configuration stored in Zookeeper.
   * @param properties Properties to configure the context.
   * @param zkClient An optional Zookeeper client.
   */
  private Context createContext(Properties properties, Optional<CuratorFramework> zkClient) throws Exception {

    Context.Builder contextBuilder = new Context.Builder()
            .with(SHELL_VARIABLES, () -> variables)
            .with(STELLAR_CONFIG, () -> properties);

    // load global configuration from zookeeper
    if (zkClient.isPresent()) {
      Map<String, Object> global = fetchGlobalConfig(zkClient.get());
      contextBuilder
              .with(GLOBAL_CONFIG, () -> global)
              .with(ZOOKEEPER_CLIENT, () -> zkClient.get())
              .with(STELLAR_CONFIG, () -> getStellarConfig(global, properties));
    }

    return contextBuilder.build();
  }

  /**
   * Fetches the global configuration from Zookeeper.
   * @param zkClient The Zookeeper client.
   * @return The global configuration retrieved from Zookeeper.
   * @throws Exception
   */
  private Map<String, Object> fetchGlobalConfig(CuratorFramework zkClient) throws Exception {
    byte[] raw = readGlobalConfigBytesFromZookeeper(zkClient);
    return JSONUtils.INSTANCE.load( new ByteArrayInputStream(raw), JSONUtils.MAP_SUPPLIER);
  }

  /**
   * @param globalConfig The global configuration.
   * @param props Property values
   * @return The Stellar configuration.
   */
  private Map<String, Object> getStellarConfig(Map<String, Object> globalConfig, Properties props) {
    Map<String, Object> stellarConfig = new HashMap<>();
    stellarConfig.putAll(globalConfig);
    if(props != null) {
      for (Map.Entry<Object, Object> kv : props.entrySet()) {
        stellarConfig.put(kv.getKey().toString(), kv.getValue());
      }
    }
    return stellarConfig;
  }

  /**
   * Executes Stellar expressions.
   * @param expression The expression to execute.
   */
  private StellarResult executeStellar(String expression) {
    StellarResult result;

    try {
      // execute the stellar expression
      VariableResolver variableResolver = new MapVariableResolver(getVariables());
      Object exprResult = new StellarProcessor().parse(expression, variableResolver, functionResolver, context);
      result = success(exprResult);

    } catch (Throwable t) {
      result = error(t);
    }

    return result;
  }
}
