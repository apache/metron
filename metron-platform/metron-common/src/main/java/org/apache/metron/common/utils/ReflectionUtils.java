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
package org.apache.metron.common.utils;

import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {

  /**
   * Creates an instance of a provided class.
   *
   * @param className The name of the class to create an instance of
   * @param defaultClass The default if the class name is null/empty or is an inner class
   * @param <T> The type parameter of the default class
   * @return An instance of the provided class.
   */
  public static <T> T createInstance(String className, T defaultClass) {
    T instance;
    if (className == null || className.length() == 0 || className.charAt(0) == '$') {
      return defaultClass;
    } else {
      instance = createInstance(className);
    }
    return instance;
  }

  /**
   * Attempts to create instance from specified class name. No-arg constructor assumed.
   *
   * @param className fully qualified name of class to instantiate. e.g. foo.bar.Baz
   * @param <T> Instance created from passed class
   * @return Object of type T
   */
  public static <T> T createInstance(String className) {
    T instance;
    try {
      Class<? extends T> clazz = (Class<? extends T>) Class.forName(className);
      instance = createInstance(clazz);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to instantiate connector: class not found", e);
    }
    return instance;
  }


  /**
   * Create instance from no-args constructor.
   *
   * @param clazz Class to create instance from
   * @param <T> Instance created from passed class
   * @return Object of type T
   */
  public static <T> T createInstance(Class<? extends T> clazz) {
    return createInstance(clazz, null, null);
  }

  /**
   * Create instance from passed class with specified parameter types and arguments. If parameter
   * types is null, defaults to attempting to instantiate the no-arg constructor.
   *
   * @param clazz Class to create instance from
   * @param parameterTypes parameter types to use for looking up the desired constructor
   * @param parameters arguments to pass into the constructor when instantiating the object.
   * @param <T> Instance created from passed class
   * @return Object of type T
   */
  public static <T> T createInstance(Class<? extends T> clazz, Class<?>[] parameterTypes, Object[] parameters) {
    T instance;
    try {
      if (parameterTypes != null) {
        instance = clazz.getConstructor(parameterTypes).newInstance(parameters);
      } else {
        instance = clazz.getConstructor().newInstance();
      }
    } catch (InstantiationException e) {
      throw new IllegalStateException("Unable to instantiate connector.", e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Unable to instantiate connector: illegal access", e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException("Unable to instantiate connector", e);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Unable to instantiate connector: no such method", e);
    }
    return instance;
  }

  /**
   * Create instance from passed class name with specified parameter types and arguments. If parameter
   * types is null, defaults to attempting to instantiate the no-arg constructor.
   *
   * @param className Class to create instance from
   * @param parameterTypes parameter types to use for looking up the desired constructor
   * @param parameters arguments to pass into the constructor when instantiating the object.
   * @param <T> Instance created from passed class
   * @return Object of type T
   */
  public static <T> T createInstance(String className, Class<?>[] parameterTypes, Object[] parameters) {
    T instance;
    try {
      Class<? extends T> clazz = (Class<? extends T>) Class.forName(className);
      instance = createInstance(clazz, parameterTypes, parameters);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to instantiate connector: class not found", e);
    }
    return instance;
  }

}
