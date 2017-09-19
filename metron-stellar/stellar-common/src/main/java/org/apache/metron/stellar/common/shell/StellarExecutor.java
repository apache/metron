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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.configuration.ConfigurationsUtils;
import org.apache.metron.stellar.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctionInfo;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.jboss.aesh.console.Console;

import static org.apache.metron.stellar.common.configuration.ConfigurationsUtils.readGlobalConfigBytesFromZookeeper;
import static org.apache.metron.stellar.common.shell.StellarExecutor.OperationType.DOC;
import static org.apache.metron.stellar.common.shell.StellarExecutor.OperationType.NORMAL;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;
import static org.apache.metron.stellar.dsl.Context.Capabilities.STELLAR_CONFIG;
import static org.apache.metron.stellar.dsl.Context.Capabilities.ZOOKEEPER_CLIENT;

/**
 * Executes Stellar expressions and maintains state across multiple invocations.
 */
public class StellarExecutor {

  public static String SHELL_VARIABLES = "shellVariables";
  public static String CONSOLE = "console";

  private ReadWriteLock indexLock = new ReentrantReadWriteLock();

  public static class VariableResult {
    private String expression;
    private Object result;

    public VariableResult(String expression, Object result) {
      this.expression = expression;
      this.result = result;
    }

    public String getExpression() {
      return expression;
    }

    public Object getResult() {
      return result;
    }

    @Override
    public String toString() {
      String ret = "" + result;
      if(expression != null) {
        ret += " via " + expression;
      }
      return ret;
    }
  }

  /**
   * Prefix tree index of auto-completes.
   */
  private PatriciaTrie<AutoCompleteType> autocompleteIndex;

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
  private Optional<CuratorFramework> client;

  /**
   * The Stellar execution context.
   */
  private Context context;

  private Console console;

  public enum OperationType {
    DOC
    , MAGIC
    , NORMAL
  }

  public interface AutoCompleteTransformation {
    String transform(OperationType type, String key);
  }

  public enum AutoCompleteType implements AutoCompleteTransformation{
      FUNCTION((type, key) -> {
        if(type == DOC) {
          return StellarShell.DOC_PREFIX + key;
        }
        else if(type == NORMAL) {
          return key + "(";
        }
        return key;
      })
    , VARIABLE((type, key) -> key )
    , TOKEN((type, key) -> key)
    ;

    AutoCompleteTransformation transform;
    AutoCompleteType(AutoCompleteTransformation transform) {
      this.transform = transform;
    }

    @Override
    public String transform(OperationType type, String key) {
      return transform.transform(type, key);
    }
  }

  /**
   * @param console The console used to drive the REPL.
   * @param properties The Stellar properties.
   * @throws Exception
   */
  public StellarExecutor(Console console, Properties properties) throws Exception {
    this(null, console, properties);
  }

  /**
   * @param console The console used to drive the REPL.
   * @param properties The Stellar properties.
   * @throws Exception
   */
  public StellarExecutor(String zookeeperUrl, Console console, Properties properties) throws Exception {
    this.variables = new HashMap<>();
    this.client = createClient(zookeeperUrl);
    this.context = createContext(properties);

    // initialize the default function resolver
    StellarFunctions.initialize(this.context);
    this.functionResolver = StellarFunctions.FUNCTION_RESOLVER();

    this.autocompleteIndex = initializeIndex();
    this.console = console;

    // asynchronously update the index with function names found from a classpath scan.
    new Thread( () -> {
      Iterable<StellarFunctionInfo> functions = functionResolver.getFunctionInfo();
      indexLock.writeLock().lock();
      try {
        for(StellarFunctionInfo info: functions) {
          String functionName = info.getName();
          autocompleteIndex.put(functionName, AutoCompleteType.FUNCTION);
        }
      } finally {
        System.out.println("Functions loaded, you may refer to functions now...");
        indexLock.writeLock().unlock();
      }
    }).start();
  }

  private PatriciaTrie<AutoCompleteType> initializeIndex() {
    Map<String, AutoCompleteType> index = new HashMap<>();

    index.put("==", AutoCompleteType.TOKEN);
    index.put(">=", AutoCompleteType.TOKEN);
    index.put("<=", AutoCompleteType.TOKEN);
    index.put(":=", AutoCompleteType.TOKEN);
    index.put("quit", AutoCompleteType.TOKEN);
    index.put(StellarShell.MAGIC_FUNCTIONS, AutoCompleteType.FUNCTION);
    index.put(StellarShell.MAGIC_VARS, AutoCompleteType.FUNCTION);
    index.put(StellarShell.MAGIC_GLOBALS, AutoCompleteType.FUNCTION);
    index.put(StellarShell.MAGIC_DEFINE, AutoCompleteType.FUNCTION);
    index.put(StellarShell.MAGIC_UNDEFINE, AutoCompleteType.FUNCTION);
    return new PatriciaTrie<>(index);
  }

  public Iterable<String> autoComplete(String buffer, final OperationType opType) {
    indexLock.readLock().lock();
    try {
      SortedMap<String, AutoCompleteType> ret = autocompleteIndex.prefixMap(buffer);
      if (ret.isEmpty()) {
        return new ArrayList<>();
      }
      return Iterables.transform(ret.entrySet(), kv -> kv.getValue().transform(opType, kv.getKey()));
    }
    finally {
      indexLock.readLock().unlock();
    }
  }

  /**
   * Creates a Zookeeper client.
   * @param zookeeperUrl The Zookeeper URL.
   */
  private Optional<CuratorFramework> createClient(String zookeeperUrl) {

    // can only create client, if have valid zookeeper URL
    if(StringUtils.isNotBlank(zookeeperUrl)) {
      CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl);
      client.start();
      return Optional.of(client);

    } else {
      return Optional.empty();
    }
  }

  /**
   * Creates a Context initialized with configuration stored in Zookeeper.
   */
  private Context createContext(Properties properties) throws Exception {

    Context.Builder contextBuilder = new Context.Builder()
            .with(SHELL_VARIABLES, () -> variables)
            .with(CONSOLE, () -> console)
            .with(STELLAR_CONFIG, () -> properties);

    // load global configuration from zookeeper
    if (client.isPresent()) {

      // fetch the global configuration
      Map<String, Object> global = JSONUtils.INSTANCE.load(
              new ByteArrayInputStream(readGlobalConfigBytesFromZookeeper(client.get())),
              new TypeReference<Map<String, Object>>() {});

      contextBuilder
              .with(GLOBAL_CONFIG, () -> global)
              .with(ZOOKEEPER_CLIENT, () -> client.get())
              .with(STELLAR_CONFIG, () -> getStellarConfig(global, properties));
    }

    return contextBuilder.build();
  }

  private Map<String, Object> getStellarConfig(Map<String, Object> globalConfig, Properties props) {
    Map<String, Object> ret = new HashMap<>();
    ret.putAll(globalConfig);
    if(props != null) {
      for (Map.Entry<Object, Object> kv : props.entrySet()) {
        ret.put(kv.getKey().toString(), kv.getValue());
      }
    }
    return ret;
  }

  /**
   * Executes the Stellar expression and returns the result.
   * @param expression The Stellar expression to execute.
   * @return The result of the expression.
   */
  public Object execute(String expression) {
    VariableResolver variableResolver = new MapVariableResolver(Maps.transformValues(variables, result -> result.getResult())
                                                               , Collections.emptyMap());
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(expression, variableResolver, functionResolver, context);
  }

  /**
   * Assigns a value to a variable.
   * @param variable The name of the variable.
   * @param value The value of the variable
   */
  public void assign(String variable, String expression, Object value) {
    this.variables.put(variable, new VariableResult(expression, value));
    indexLock.writeLock().lock();
    try {
      if (value != null) {
        this.autocompleteIndex.put(variable, AutoCompleteType.VARIABLE);
      } else {
        this.autocompleteIndex.remove(variable);
      }
    }
    finally {
      indexLock.writeLock().unlock();
    }
  }

  public Map<String, VariableResult> getVariables() {
    return this.variables;
  }

  public FunctionResolver getFunctionResolver() {
    return functionResolver;
  }

  public Context getContext() {
    return context;
  }
}

