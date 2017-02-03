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
package org.apache.metron.dataloads.extractor;

import org.apache.metron.dataloads.extractor.inputformat.Formats;
import org.apache.metron.dataloads.extractor.inputformat.InputFormatHandler;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Map;

public class ExtractorHandler {
  final static ObjectMapper _mapper = new ObjectMapper();
  private Map<String, Object> config;
  private Extractor extractor;
  private InputFormatHandler inputFormat = Formats.BY_LINE;

  public Map<String, Object> getConfig() {
    return config;
  }

  /**
   * Set by jackson. Extractor configuration from JSON
   */
  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  public InputFormatHandler getInputFormat() {
    return inputFormat;
  }

  /**
   * Set by jackson
   */
  public void setInputFormat(String handler) {
    try {
      this.inputFormat = Formats.create(handler);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to create an inputformathandler", e);
    }
  }

  public Extractor getExtractor() {
    return extractor;
  }

  /**
   * Set by jackson.
   *
   * @param extractor Name of extractor to instantiate
   */
  public void setExtractor(String extractor) {
    try {
      this.extractor = Extractors.create(extractor);
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to create an extractor", e);
    }
  }

  /**
   * Load json configuration
   */
  public static synchronized ExtractorHandler load(InputStream is) throws IOException {
    ExtractorHandler ret = _mapper.readValue(is, ExtractorHandler.class);
    ret.getExtractor().initialize(ret.getConfig());
    return ret;
  }

  /**
   * Load json configuration
   */
  public static synchronized ExtractorHandler load(String s, Charset c) throws IOException {
    return load(new ByteArrayInputStream(s.getBytes(c)));
  }

  /**
   * Load json configuration
   */
  public static synchronized ExtractorHandler load(String s) throws IOException {
    return load(s, Charset.defaultCharset());
  }
}
