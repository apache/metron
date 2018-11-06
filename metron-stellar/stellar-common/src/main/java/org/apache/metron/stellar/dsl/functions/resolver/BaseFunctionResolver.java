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

package org.apache.metron.stellar.dsl.functions.resolver;

import static java.lang.String.format;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.ObjectUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.stellar.dsl.StellarFunctionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base implementation of a function resolver that provides a means for lazy
 * initialization, thread-safety, and a mechanism for function resolution.
 *
 * Concrete function resolvers can override the `resolvables` method which
 * defines the classes that are interrogated further to discover Stellar functions.
 */
public abstract class BaseFunctionResolver implements FunctionResolver, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Maps a function name to the metadata necessary to execute the Stellar function.
   */
  private Supplier<Map<String, StellarFunctionInfo>> functions;

  /**
   * The Stellar execution context that can be used to inform the function resolution process.
   */
  protected Context context;

  /**
   * Indicates if closed has been called on this resolver.
   */
  private boolean closed;

  public BaseFunctionResolver() {
    // memoize provides lazy initialization and thread-safety (the ugly cast is necessary for serialization)
    functions = Suppliers.memoize((Supplier<Map<String, StellarFunctionInfo>> & Serializable) this::resolveFunctions);
    closed = false;
  }

  /**
   * Returns a set of classes that should undergo further interrogation for resolution
   * (aka discovery) of Stellar functions.
   */
  public abstract Set<Class<? extends StellarFunction>> resolvables();

  /**
   * Provides metadata about each Stellar function that is resolvable.
   */
  @Override
  public Iterable<StellarFunctionInfo> getFunctionInfo() {
    return functions.get().values();
  }

  /**
   * The names of all Stellar functions that are resolvable.
   */
  @Override
  public Iterable<String> getFunctions() {
    return functions.get().keySet();
  }

  /**
   * Initialize the function resolver.
   * @param context Context used to initialize.
   */
  @Override
  public void initialize(Context context) {
    this.context = context;
  }

  /**
   * Makes an attempt to close all Stellar functions. Calling close multiple times has no effect.
   * @throws IOException Catches all exceptions and summarizes them.
   */
  @Override
  public void close() throws IOException {
    if (!closed) {
      LOG.info("Calling close() on Stellar functions.");
      Map<String, Throwable> errors = new HashMap<>();
      for (StellarFunctionInfo info : getFunctionInfo()) {
        try {
          info.getFunction().close();
        } catch (Throwable t) {
          errors.put(info.getName(), t);
        }
      }
      if (!errors.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unable to close Stellar functions:");
        for (Map.Entry<String, Throwable> e : errors.entrySet()) {
          Throwable throwable = e.getValue();
          String eText = String
              .format("Exception - Function: %s; Message: %s; Cause: %s", e.getKey(),
                  throwable.getMessage(),
                  throwable.getCause());
          sb.append(System.lineSeparator());
          sb.append(eText);
        }
        closed = true;
        throw new IOException(sb.toString());
      }
      closed = true;
    } else {
      LOG.info("close() already called on Stellar functions - skipping.");
    }
  }

  /**
   * Resolves a function by name.
   * @param functionName The name of the function to resolve.
   * @return The executable StellarFunction.
   */
  @Override
  public StellarFunction apply(String functionName) {
    StellarFunctionInfo info = functions.get().get(functionName);
    if(info == null) {
      throw new IllegalStateException(format("Unknown function: `%s`", functionName));
    }
    return info.getFunction();
  }

  /**
   * Performs the core process of function resolution.
   */
  protected Map<String, StellarFunctionInfo> resolveFunctions() {

    // maps a function name to its definition
    Map<String, StellarFunctionInfo> functions = new HashMap<>();

    for(Class<? extends StellarFunction> clazz : resolvables()) {
      StellarFunctionInfo fn = resolveFunction(clazz);
      if(fn != null) {
        // check for duplicate function names
        StellarFunctionInfo fnSameName = functions.get(fn.getName());
        if (fnSameName != null && ObjectUtils.notEqual(fnSameName, fn)) {
          LOG.warn("Namespace conflict: duplicate function names; `{}` implemented by [{}, {}]",
              fn.getName(), fnSameName.getFunction(), fn.getFunction());
        }

        functions.put(fn.getName(), fn);
      }
    }

    return functions;
  }

  /**
   * Resolves a Stellar function from a given class.
   * @param clazz The class.
   */
  public static StellarFunctionInfo resolveFunction(Class<? extends StellarFunction> clazz) {
    StellarFunctionInfo info = null;

    // the class must be annotated
    if (clazz.isAnnotationPresent(Stellar.class)) {

      Stellar annotation = clazz.getAnnotation(Stellar.class);
      String fullyQualifiedName = getNameFromAnnotation(annotation);
      StellarFunction function = createFunction(clazz);

      if (fullyQualifiedName != null && function != null) {
        info = new StellarFunctionInfo(
                annotation.description(),
                fullyQualifiedName,
                annotation.params(),
                annotation.returns(),
                function);
      }
    }

    return info;
  }

  /**
   * Returns the fully-qualified function name from a Stellar annotation.
   * @param annotation The Stellar annotation.
   */
  public static String getNameFromAnnotation(Stellar annotation) {

    // find the function name
    String name = annotation.name();
    if(name == null || name.trim().length() == 0) {
      return null;
    } else {
      name = name.trim();
    }

    // find the function namespace
    String namespace = annotation.namespace();
    if(namespace == null || namespace.length() == 0) {
      namespace = null;
    } else {
      namespace = namespace.trim();
    }

    return Joiner.on("_").skipNulls().join(Arrays.asList(namespace, name));
  }

  /**
   * Instantiate the StellarFunction implementation class.
   * @param clazz The class containing a Stellar function definition.
   */
  public static StellarFunction createFunction(Class<? extends StellarFunction> clazz) {
    try {
      return clazz.getConstructor().newInstance();

    } catch (Exception e) {
      LOG.error("Unable to load {} because {}", clazz.getName(), e.getMessage(), e);
      return null;
    }
  }
}
