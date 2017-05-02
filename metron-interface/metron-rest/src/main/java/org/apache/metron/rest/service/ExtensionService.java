/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.rest.service;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.extensions.ParserExtensionConfig;
import org.apache.metron.rest.RestException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ExtensionService {
  public enum ExtensionType{
    PARSER,
    STELLAR
  }
  public enum Paths {
    BUNDLE,
    CONFIG,
    ZOOKEEPER,
    ENRICHMENTS_CONFIG_DIR,
    INDEXING_CONFIG_DIR,
    PARSERS_CONFIG_DIR,
    ENRICHMENTS_CONFIG,
    INDEXING_CONFIG,
    PARSERS_CONFIG,
    GROK_DIR,
    GROK_RULES,
    ELASTICSEARCH_DIR,
    ELASTICSEARCH_TEMPLATES
    //, SEARCH, ELASTICSEARCH, SOLR, LOGROTATE
  }
  void install(ExtensionType extensionType, String extensionPackageName, TarArchiveInputStream tgzStream) throws Exception;
  ParserExtensionConfig findOneParserExtension(String name) throws RestException;
  Map<String, ParserExtensionConfig> getAllParserExtensions() throws RestException;
  List<String> getAllParserExtensionTypes() throws RestException;
  boolean deleteParserExtension(String name) throws RestException;
}
