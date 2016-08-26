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
package org.apache.metron.common.dsl;

import com.google.common.base.Joiner;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FunctionResolverSingleton implements FunctionResolver {
  protected static final Logger LOG = LoggerFactory.getLogger(FunctionResolverSingleton.class);
  private final Map<String, StellarFunctionInfo> functions = new HashMap<>();
  private final AtomicBoolean isInitialized = new AtomicBoolean(false);
  private static final ReadWriteLock lock = new ReentrantReadWriteLock();
  private static FunctionResolverSingleton INSTANCE = new FunctionResolverSingleton();

  private FunctionResolverSingleton() {}

  public static FunctionResolver getInstance() {
    return INSTANCE;
  }



  @Override
  public Iterable<StellarFunctionInfo> getFunctionInfo() {
    return _getFunctions().values();
  }

  @Override
  public Iterable<String> getFunctions() {
    return _getFunctions().keySet();
  }

  @Override
  public void initialize(Context context) {
    //forces a load of the stellar functions.
    _getFunctions();
  }

  /**
   * This allows the lazy loading of the functions.  We do not want to take a multi-second hit to analyze the full classpath
   * every time a unit test starts up.  That would cause the runtime of things to blow right up.  Instead, we only want
   * to take the hit if a function is actually called from a stellar expression.
   *
   *
   * @return The map of the stellar functions that we found on the classpath indexed by fully qualified name
   */
  private Map<String, StellarFunctionInfo> _getFunctions() {
    /*
     * Because we are not doing this in a static block and because this object is a singleton we have to concern ourselves with
     * the possiblity that two threads are calling this function at the same time.  Normally, I would consider just making the
     * function synchronized, but since every stellar statement which uses a function will be here, I wanted to distinguish
     * between read locks (that happen often and are quickly resolved) and write locks (which should happen at initialization time).
     */
    lock.readLock().lock();
    try {
      if (isInitialized.get()) {
        return functions;
      }
    }
    finally {
      lock.readLock().unlock();
    }
    //we should VERY rarely get here.
    lock.writeLock().lock();
    try {
      //I have to check again because two threads or more could be waiting at the lock statement.  The loser threads
      //shouldn't reinitialize.
      if(!isInitialized.get()) {
        loadFunctions(functions);
        isInitialized.set(true);
      }
      return functions;
    }
    finally {
      lock.writeLock().unlock();
    }
  }
  /**
   * Applies this function to the given argument.
   *
   * @param s the function argument
   * @return the function result
   */
  @Override
  public StellarFunction apply(String s) {
    StellarFunctionInfo ret = _getFunctions().get(s);
    if(ret == null) {
      throw new IllegalStateException("Unable to resolve function " + s);
    }
    return ret.getFunction();
  }

  private void loadFunctions(final Map<String, StellarFunctionInfo> ret) {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(effectiveClassPathUrls(classLoader)));
      for (Class<?> clazz : reflections.getSubTypesOf(StellarFunction.class)) {
        if (clazz.isAnnotationPresent(Stellar.class)) {
          Map.Entry<String, StellarFunctionInfo> instance = create((Class<? extends StellarFunction>) clazz);
          if (instance != null) {
            if(ret.containsKey(instance.getKey()) && !ret.get(instance.getKey()).equals(instance.getValue())) {
              throw new IllegalStateException("You have a namespace conflict: two functions named " + instance.getKey());
            }
            ret.put(instance.getKey(), instance.getValue());
          }
        }
      }
      LOG.info("Found " + ret.size() + " Stellar Functions...");
    }
    catch(Throwable ex) {
      LOG.error("Unable to initialize FunctionResolverImpl: " + ex.getMessage(), ex);
      throw ex;
    }
  }

  /**
   * To handle the situation where classpath is specified in the manifest of the jar, we have to augment the URLs.
   * This happens as part of the surefire plugin as well as elsewhere in the wild.
   * @param classLoaders
   * @return
   */
  public static Collection<URL> effectiveClassPathUrls(ClassLoader... classLoaders) {
    return ClasspathHelper.forManifest(ClasspathHelper.forClassLoader(classLoaders));
  }



  private static Map.Entry<String, StellarFunctionInfo> create(Class<? extends StellarFunction> stellarClazz) {
    String fqn = getNameFromAnnotation(stellarClazz);
    if(fqn == null) {
      LOG.error("Unable to resolve fully qualified stellar name for " + stellarClazz.getName());
    }
    StellarFunction f = createFunction(stellarClazz);
    if(fqn != null && f != null) {
      Stellar stellarAnnotation = stellarClazz.getAnnotation(Stellar.class);
      StellarFunctionInfo info = new StellarFunctionInfo(stellarAnnotation.description()
                                                        , fqn
                                                        , stellarAnnotation.params()
                                                        , stellarAnnotation.returns()
                                                        , f
                                                        );
      return new AbstractMap.SimpleEntry<>(fqn, info);
    }
    else {
      LOG.error("Unable to create instance for StellarFunction " + stellarClazz.getName() + " name: " + fqn);
    }
    return null;
  }

  private static String getNameFromAnnotation(Class<? extends StellarFunction> stellarClazz) {
    if(stellarClazz.isAnnotationPresent(Stellar.class)) {
      Stellar stellarAnnotation = stellarClazz.getAnnotation(Stellar.class);
      String namespace = stellarAnnotation.namespace();
      String name = stellarAnnotation.name();
      if(name == null || name.trim().length() == 0) {
        return null;
      }
      else {
        name = name.trim();
      }
      if(namespace == null || namespace.length() == 0) {
        namespace = null;
      }
      else {
        namespace = namespace.trim();
      }
      return Joiner.on("_").skipNulls().join(Arrays.asList(namespace, name));
    }
    return null;

  }

  private static StellarFunction createFunction(Class<? extends StellarFunction> stellarClazz) {
    try {
      return stellarClazz.newInstance();
    } catch (Exception e) {
      LOG.error("Unable to load " + stellarClazz.getName() + " because " + e.getMessage(), e);
      return null;
    }
  }
}
