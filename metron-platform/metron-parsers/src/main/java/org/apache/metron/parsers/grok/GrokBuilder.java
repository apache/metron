/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.parsers.grok;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.commons.lang.StringUtils;
import org.apache.metron.common.utils.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p> GrokBuilder builds an instance of Grok with patterns loaded. </p> Either the Parser
 * Configuration, with keys : grokPath and patternLabel must be passed or those values must be
 * provided using the with statements.
 */
public class GrokBuilder {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Map<String, Object> parserConfiguration;
  protected String patternsCommonDir = "/patterns/common";
  protected List<Reader> readers = new ArrayList<>();
  protected String grokPath;
  protected String patternLabel;
  protected boolean loadCommon = true;

  /**
   * Constructor.
   */
  public GrokBuilder() {
  }

  /**
   * GrokBuilder with a parserConfiguration.
   *
   * @param parserConfiguration the configuration
   * @return GrokBuilder
   */
  public GrokBuilder withParserConfiguration(Map<String, Object> parserConfiguration) {
    this.parserConfiguration = parserConfiguration;
    return this;
  }

  /**
   * GrokBuilder with a {@link Reader} to read the file input.
   *
   * @param reader a Reader instance
   * @return the Builder
   */
  public GrokBuilder withReader(Reader reader) {
    this.readers.add(reader);
    return this;
  }

  /**
   * GrokBuilder with a grokPath.
   *
   * @param grokPath the grokPath
   * @return the Builder
   */
  public GrokBuilder withGrokPath(String grokPath) {
    if (StringUtils.isEmpty(grokPath)) {
      throw new IllegalArgumentException("grokPath cannot be empty");
    }
    this.grokPath = grokPath;
    return this;
  }

  /**
   * GrokBuilder with a given pattern label.
   *
   * @param patternLabel the pattern label
   * @return the Builder
   */
  public GrokBuilder withPatternLabel(String patternLabel) {
    if (StringUtils.isEmpty(patternLabel)) {
      throw new IllegalArgumentException("patternLabel cannot be empty");
    }
    this.patternLabel = patternLabel;
    return this;
  }

  /**
   * GrokBuilder with a given patterns common directory.
   *
   * @param patternsCommonDir the pattern common directory
   * @return the Builder
   */
  public GrokBuilder withPatternsCommonDir(String patternsCommonDir) {
    this.patternsCommonDir = patternsCommonDir;
    return this;
  }

  /**
   * Flag to try to load common grok file.
   *
   * @param loadCommon flag, if true load if false do not
   * @return the Builder
   */
  public GrokBuilder withLoadCommon(boolean loadCommon) {
    this.loadCommon = loadCommon;
    return this;
  }

  /**
   * Builds a fully configured Grok. Fully configured means that the Grok has all of the configured
   * patterns loaded.
   *
   * @return Grok
   * @throws Exception if there are problems loading or compiling the patterns into Grok
   */
  @SuppressWarnings("unchecked")
  public Grok build() throws Exception {
    if (parserConfiguration != null) {
      if (parserConfiguration.containsKey("grokPath")) {
        grokPath = (String) parserConfiguration.get("grokPath");
      }
      if (parserConfiguration.containsKey("patternLabel")) {
        patternLabel = (String) parserConfiguration.get("patternLabel");
      }
      if (parserConfiguration.containsKey("readers")) {
        List<Reader> configReaders = (List<Reader>) parserConfiguration.get("readers");
        this.readers.addAll(configReaders);
      }
      if (parserConfiguration.containsKey("loadCommon")) {
        this.loadCommon = (Boolean) parserConfiguration.get("loadCommon");
      }
    } else {
      if (StringUtils.isEmpty(grokPath) && readers.size() == 0) {
        throw new IllegalArgumentException("missing required grokPath");
      }
      if (StringUtils.isEmpty(patternLabel) && readers.size() == 0) {
        throw new IllegalArgumentException("missing required patternLabel");
      }
    }

    Grok grok = new Grok();
    Map<String, Object> config = parserConfiguration;
    if (config == null) {
      config = new HashMap<>();
    }
    Object globalObject = config.get("globalConfig");
    Map<String, Object> globalConfig = null;
    if (globalObject != null) {
      globalConfig = (Map<String, Object>) globalObject;
    }

    try (ResourceLoader resourceLoader = new ResourceLoader.Builder()
        .withConfiguration(globalConfig).build()) {
      Map<String, InputStream> streamMap = null;
      if (loadCommon) {
        LOG.debug("Grok parser loading common patterns from: {}", patternsCommonDir);
        streamMap = resourceLoader.getResources(patternsCommonDir);
        load(grok, streamMap);
      }
      if (readers.size() == 0) {
        if (StringUtils.isNotEmpty(grokPath)) {
          LOG.debug("Loading parser-specific patterns from: {}", grokPath);

          streamMap = resourceLoader.getResources(grokPath);

          load(grok, streamMap);
        }
      } else {
        LOG.debug("Loading pattern for reader");
        readers.forEach(x -> {
          try {
            load(grok, x);
          } catch (GrokException e) {
            LOG.error("error loading grok pattern from reader", e);
            throw new IllegalStateException(e);
          }

        });
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Grok parser set the following grok expression: {}",
            grok.getNamedRegexCollectionById(patternLabel));
      }
      if (StringUtils.isNotEmpty(patternLabel)) {
        String grokPattern = "%{" + patternLabel + "}";
        grok.compile(grokPattern);
        LOG.debug("Compiled grok pattern {}", grokPattern);
      }

      return grok;
    }
  }

  private void load(Grok grok, Map<String, InputStream> streams) throws IOException, GrokException {
    for (Entry<String, InputStream> entry : streams.entrySet()) {
      try (InputStream thisStream = entry.getValue()) {
        if (thisStream == null) {
          throw new RuntimeException(
              "Unable to initialize grok parser: Unable to load " + entry.getKey()
                  + " from either HDFS or locally");
        }
        LOG.debug("Loading patterns from: {}", entry.getKey());
        grok.addPatternFromReader(new InputStreamReader(thisStream));
      }
    }
  }

  private void load(Grok grok, Reader reader) throws GrokException {
    grok.addPatternFromReader(reader);
  }

}
