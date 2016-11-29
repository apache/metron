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

package org.apache.metron.common.dsl.functions.resolver;

import org.apache.commons.lang.StringUtils;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunction;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.apache.metron.common.dsl.Context.Capabilities.STELLAR_CONFIG;

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

  /**
   * The key for a global property that defines one or more regular expressions
   * that specify what should be included when searching for Stellar functions.
   */
  public static final String STELLAR_SEARCH_INCLUDES_KEY = "stellar.function.resolver.includes";

  /**
   * The key for a global property that defines one or more regular expressions
   * that specify what should be excluded when searching for Stellar functions.
   */
  public static final String STELLAR_SEARCH_EXCLUDES_KEY = "stellar.function.resolver.excludes";

  /**
   * The includes and excludes can include a list of multiple includes or excludes that
   * are delimited by these values.
   */
  protected static final String STELLAR_SEARCH_DELIMS = "[,:]";

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

  public ClasspathFunctionResolver() {
    this.includes = new ArrayList<>();
    this.excludes = new ArrayList<>();
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
  public void initialize(Context context) {
    super.initialize(context);
    if(context != null) {

      Optional<Object> optional = context.getCapability(STELLAR_CONFIG, false);
      if (optional.isPresent()) {
        Map<String, Object> stellarConfig = (Map<String, Object>) optional.get();

        // handle any includes
        String includes = (String) stellarConfig.getOrDefault(STELLAR_SEARCH_INCLUDES_KEY, "");
        if(StringUtils.isNotBlank(includes)) {
          include(includes.split(STELLAR_SEARCH_DELIMS));
        }

        // handle any excludes
        String excludes = (String) stellarConfig.getOrDefault(STELLAR_SEARCH_EXCLUDES_KEY, "");
        if(StringUtils.isNotBlank(excludes)) {
          exclude(excludes.split(STELLAR_SEARCH_DELIMS));
        }
      }
    }
  }

  /**
   * Returns a set of classes that should undergo further interrogation for resolution
   * (aka discovery) of Stellar functions.
   */
  @Override
  protected Set<Class<? extends StellarFunction>> resolvables() {

    ClassLoader classLoader = getClass().getClassLoader();
    Collection<URL> searchPath = effectiveClassPathUrls(classLoader);

    FilterBuilder filterBuilder = new FilterBuilder();
    excludes.forEach(excl -> filterBuilder.exclude(excl));
    includes.forEach(incl -> filterBuilder.include(incl));

    Reflections reflections = new Reflections(
            new ConfigurationBuilder()
                    .setUrls(searchPath)
                    .filterInputsBy(filterBuilder));
    return reflections.getSubTypesOf(StellarFunction.class);
  }

  /**
   * To handle the situation where classpath is specified in the manifest of the
   * jar, we have to augment the URLs.  This happens as part of the surefire plugin
   * as well as elsewhere in the wild.
   * @param classLoaders
   */
  public static Collection<URL> effectiveClassPathUrls(ClassLoader... classLoaders) {
    return ClasspathHelper.forManifest(ClasspathHelper.forClassLoader(classLoaders));
  }
}
