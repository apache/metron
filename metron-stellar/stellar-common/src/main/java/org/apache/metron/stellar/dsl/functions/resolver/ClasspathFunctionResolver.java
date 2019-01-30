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

import static org.apache.metron.stellar.dsl.Context.Capabilities.STELLAR_CONFIG;
import static org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver.Config.STELLAR_SEARCH_EXCLUDES_KEY;
import static org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver.Config.STELLAR_SEARCH_INCLUDES_KEY;
import static org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver.Config.STELLAR_VFS_PATHS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.VFSClassLoader;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.VFSClassloaderUtil;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.atteo.classindex.ClassIndex;
import org.reflections.util.FilterBuilder;

/**
 * Performs function resolution for Stellar by searching the classpath.
 *
 * By default, the entire classpath will be searched for Stellar functions.  At times,
 * this can take quite a while.  To shorten the search time, a property can be
 * defined to either include or exclude certain packages.  The fewer packages there are
 * to search, the quicker the search will be.
 *
 * The properties are pulled from the Context's 'STELLAR_CONFIG'. In the REPL, this
 * is defined in a file called 'stellar.properties' on the classpath.
 *
 * The following property definition will include only Stellar functions that are
 * part of Apache Metron.
 *
 *   stellar.function.resolver.includes = org.apache.metron.*
 *
 * The following property definition will exclude Stellar functions that are part of
 * Metron's management suite of function.
 *
 *   stellar.function.resolver.excludes = org.apache.metron.management.*
 *
 * The following property definition would also exclude the Stellar functions that are
 * part of the management suite of functions.  Of course, this may also exclude other
 * packages, but this serves as an example of the types of expression that can be used.
 *
 *   stellar.function.resolver.excludes = org\\.management.*
 *
 */
public class ClasspathFunctionResolver extends BaseFunctionResolver {
  public enum Config {
    /**
     * The set of paths.  These paths are comma separated URLs with optional regex patterns at the end.
     * e.g. hdfs://node1:8020/apps/metron/stellar/.*.jar,hdfs://node1:8020/apps/metron/my_org/.*.jar
     * would signify all the jars under /apps/metron/stellar and /apps/metron/my_org in HDFS.
     */
    STELLAR_VFS_PATHS("stellar.function.paths", ""),
    /**
     * The key for a global property that defines one or more regular expressions
     * that specify what should be included when searching for Stellar functions.
     */
    STELLAR_SEARCH_INCLUDES_KEY("stellar.function.resolver.includes", ""),
    /**
     * The key for a global property that defines one or more regular expressions
     * that specify what should be excluded when searching for Stellar functions.
     */
    STELLAR_SEARCH_EXCLUDES_KEY("stellar.function.resolver.excludes", ""),


    ;
    String param;
    Object defaultValue;
    Config(String param, String defaultValue) {
      this.param = param;
      this.defaultValue = defaultValue;
    }

    public String param() {
      return param;
    }

    public Object get(Map<String, Object> config) {
      return config.getOrDefault(param, defaultValue);
    }

    public <T> T get(Map<String, Object> config, Class<T> clazz) {
      return ConversionUtils.convert(get(config), clazz);
    }
  }


  /**
   * The includes and excludes can include a list of multiple includes or excludes that
   * are delimited by these values.
   */
  private static final String STELLAR_SEARCH_DELIMS = "[,:]";


  /**
   * Regular expressions defining packages that should be included in the Stellar function resolution
   * process.
   */
  private List<String> includes;

  /**
   * Regular expressions defining packages that should be excluded from the Stellar function resolution
   * process.
   */
  private List<String> excludes;

  /**
   * Classloaders to try to load from
   */
  private List<ClassLoader> classLoaders;

  public ClasspathFunctionResolver() {
    this.includes = new ArrayList<>();
    this.excludes = new ArrayList<>();
    this.classLoaders = new ArrayList<>();
  }

  /**
   * Use one or more classloaders
   * @param classloaders
   */
  public void classLoaders(ClassLoader... classloaders) {
    classLoaders.clear();
    Arrays.stream(classloaders).forEach(c -> classLoaders.add(c));
  }

  /**
   * Includes one or more packages in the Stellar function resolution process.  The packages
   * to include can be specified with a regular expression.
   * @param toInclude The regular expressions.
   */
  public void include(String... toInclude) {
    for(String incl : toInclude) {
      includes.add(incl);
    }
  }

  /**
   * Excludes one or more packages from the Stellar function resolution process.  The packages
   * to exclude can be specified with a regular expression.
   * @param toExclude The regular expressions defining packages that should be excluded.
   */
  public void exclude(String... toExclude) {
    for(String excl : toExclude) {
      excludes.add(excl);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void initialize(Context context) {
    super.initialize(context);
    if (context != null) {

      Optional<Object> optional = context.getCapability(STELLAR_CONFIG, false);
      if (optional.isPresent()) {
        Map<String, Object> stellarConfig = (Map<String, Object>) optional.get();
        if(LOG.isDebugEnabled()) {
          LOG.debug("Setting up classloader using the following config: {}", stellarConfig);
        }

        include(STELLAR_SEARCH_INCLUDES_KEY.get(stellarConfig, String.class).split(STELLAR_SEARCH_DELIMS));
        exclude(STELLAR_SEARCH_EXCLUDES_KEY.get(stellarConfig, String.class).split(STELLAR_SEARCH_DELIMS));
        Optional<ClassLoader> vfsLoader = Optional.empty();
        try {
          vfsLoader = VFSClassloaderUtil.configureClassloader(STELLAR_VFS_PATHS.get(stellarConfig, String.class));
          if (vfsLoader.isPresent()) {
            LOG.debug("CLASSLOADER LOADED WITH: {}", STELLAR_VFS_PATHS.get(stellarConfig, String.class));
            if(LOG.isDebugEnabled()) {
              for (FileObject fo : ((VFSClassLoader) vfsLoader.get()).getFileObjects()) {
                LOG.error("{} - {}", fo.getURL(), fo.exists());
              }
            }
            classLoaders(vfsLoader.get());
          }
        } catch (FileSystemException e) {
          LOG.error("Unable to process filesystem: {}", e.getMessage(), e);
        }
      } else {
        LOG.info("No stellar config set; I'm reverting to the context classpath with no restrictions.");
        if (LOG.isDebugEnabled()) {
          try {
            throw new IllegalStateException("No config set, stacktrace follows.");
          } catch (IllegalStateException ise) {
            LOG.error(ise.getMessage(), ise);
          }
        }
      }
    } else {
      throw new IllegalStateException("CONTEXT IS NULL!");
    }
  }

  protected Iterable<Class<?>> getStellarClasses(ClassLoader cl) {
    return ClassIndex.getAnnotated(Stellar.class, cl);
  }

  protected boolean includeClass(Class<?> c, FilterBuilder filterBuilder)
  {
    boolean isAssignable = StellarFunction.class.isAssignableFrom(c);
    boolean isFiltered = filterBuilder.apply(c.getCanonicalName());
    return isAssignable && isFiltered;
  }

  /**
   * Returns a set of classes that should undergo further interrogation for resolution
   * (aka discovery) of Stellar functions.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<Class<? extends StellarFunction>> resolvables() {

    ClassLoader[] cls = null;
    if (this.classLoaders.size() == 0) {
      LOG.warn("Using System classloader");
      cls = new ClassLoader[]{getClass().getClassLoader()};
    } else {
      List<ClassLoader> classLoaderList = new ArrayList<>();
      for (int i = 0; i < this.classLoaders.size(); ++i) {
        ClassLoader cl = this.classLoaders.get(i);
        if (null != cl) {
          LOG.debug("Using classloader: {}", cl.getClass().getCanonicalName());
          classLoaderList.add(cl);
        } else {
          LOG.error(
              "This should not happen, so report a bug if you see this error - Classloader {} of {} was null. Classloader list is: {}",
              i, this.classLoaders.size(), this.classLoaders);
        }
      }
      cls = classLoaderList.toArray(new ClassLoader[0]);
    }

    FilterBuilder filterBuilder = new FilterBuilder();
    excludes.forEach(excl -> {
      if (excl != null) {
        filterBuilder.exclude(excl);
      }
    });
    includes.forEach(incl -> {
      if (incl != null) {
        filterBuilder.include(incl);
      }
    });
    Set<String> classes = new HashSet<>();
    Set<Class<? extends StellarFunction>> ret = new HashSet<>();
    for (ClassLoader cl : cls) {
      for (Class<?> c : getStellarClasses(cl)) {
        try {
          LOG.debug("{}: Found class: {}", cl.getClass().getCanonicalName(), c.getCanonicalName());
          if (includeClass(c, filterBuilder)) {
            String className = c.getName();
            if (!classes.contains(className)) {
              LOG.debug("{}: Added class: {}", cl.getClass().getCanonicalName(), className);
              ret.add((Class<? extends StellarFunction>) c);
              classes.add(className);
            }
          }
        } catch (Error le) {
          //we have had some error loading a stellar function.  This could mean that
          //the classpath is unstable (e.g. old copies of jars are on the classpath).
          try {
            LOG.error("Skipping class " + c.getName() + ": " + le.getMessage()
                    + ", please check that there are not old versions of stellar functions on the classpath.", le);
          } catch (Error ie) {
            //it's possible that getName() will throw an exception if the class is VERY malformed.
            LOG.error("Skipping class: " + le.getMessage()
                    + ", please check that there are not old versions of stellar functions on the classpath.", le);
          }
        }
      }
    }
    return ret;
  }

}
