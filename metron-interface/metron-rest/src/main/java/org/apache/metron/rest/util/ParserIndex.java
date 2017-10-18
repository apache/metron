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
package org.apache.metron.rest.util;

import org.apache.metron.parsers.interfaces.MessageParser;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Index the parsers.  Analyzing the classpath is a costly operation, so caching it makes sense.
 * Eventually, we will probably want to have a timer that periodically reindexes so that new parsers show up.
 */
public enum ParserIndex {
  INSTANCE;
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static Set<Class<? extends MessageParser>> index;
  private static Map<String, String> availableParsers ;

  static {
    load();
  }

  public synchronized Map<String, String> getIndex() {
    if(availableParsers == null) {
      load();
    }
    return availableParsers;
  }

  public synchronized Set<Class<? extends MessageParser>> getClasses() {
    if(index == null) {
      load();
    }
    return index;
  }

  public static void reload() {
    load();
  }

  /**
   * To handle the situation where classpath is specified in the manifest of the jar, we have to augment the URLs.
   * This happens as part of the surefire plugin as well as elsewhere in the wild (especially in maven when running tests. ;).
   * @param classLoaders
   * @return A collection of URLs representing the effective classpath URLs
   */
  private static Collection<URL> effectiveClassPathUrls(ClassLoader... classLoaders) {
    return ClasspathHelper.forManifest(ClasspathHelper.forClassLoader(classLoaders));
  }

  private static synchronized void load() {
    LOG.debug("Starting Parser Index Load");
    ClassLoader classLoader = ParserIndex.class.getClassLoader();
    Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(effectiveClassPathUrls(classLoader)));
    Set<Class<? extends MessageParser>> indexLoc = reflections.getSubTypesOf(MessageParser.class);
    Map<String, String> availableParsersLoc = new HashMap<>();
    indexLoc.forEach(parserClass -> {
      if (!"BasicParser".equals(parserClass.getSimpleName())) {
        availableParsersLoc.put(parserClass.getSimpleName().replaceAll("Basic|Parser", ""),
                parserClass.getName());
      }
    });
    LOG.debug("Finished Parser Index Load; found {} parsers, indexed {} parsers", indexLoc.size(), availableParsersLoc.size());
    index = indexLoc;
    availableParsers = availableParsersLoc;
  }
}
