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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ExtractorHandler {
    final static ObjectMapper _mapper = new ObjectMapper();
    private Map<String, Object> config;
    private Extractor extractor;
    private InputFormatHandler inputFormatHandler = Formats.BY_LINE;

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public InputFormatHandler getInputFormatHandler() {
        return inputFormatHandler;
    }

    public void setInputFormatHandler(String handler) {
        try {
            this.inputFormatHandler= Formats.create(handler);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to create an inputformathandler", e);
        }
    }

    public Extractor getExtractor() {
        return extractor;
    }
    public void setExtractor(String extractor) {
        try {
            this.extractor = Extractors.create(extractor);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Unable to create an extractor", e);
        }
    }

    public static synchronized ExtractorHandler load(InputStream is) throws IOException {
        ExtractorHandler ret = _mapper.readValue(is, ExtractorHandler.class);
        ret.getExtractor().initialize(ret.getConfig());
        return ret;
    }
    public static synchronized ExtractorHandler load(String s, Charset c) throws IOException {
        return load( new ByteArrayInputStream(s.getBytes(c)));
    }
    public static synchronized ExtractorHandler load(String s) throws IOException {
        return load( s, Charset.defaultCharset());
    }
}
