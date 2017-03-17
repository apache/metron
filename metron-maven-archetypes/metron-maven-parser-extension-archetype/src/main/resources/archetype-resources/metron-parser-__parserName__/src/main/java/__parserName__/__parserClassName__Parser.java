#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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

package ${package}.${parserName};

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.guava.collect.ImmutableList;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class ${parserClassName}Parser extends BasicParser {

  protected static final Logger _LOG = LoggerFactory.getLogger(${parserClassName}Parser.class);

  @Override
  public void configure(Map<String, Object> parserConfig) {

  }

  @Override
  public void init() {

  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JSONObject> parse(byte[] msg) {

    _LOG.trace("[Metron] Starting to parse incoming message");

    String originalString = new String(msg);
    try {
      //convert the JSON blob into a String -> Object map
      Map<String, Object> payload = JSONUtils.INSTANCE.load(originalString, new TypeReference<Map<String, Object>>() {
      });
      _LOG.trace("[Metron] Received message: " + originalString);
      payload.put("original_string", originalString);

      if(!payload.containsKey("timestamp")) {
        //we have to ensure that we have a timestamp.  This is one of the pre-requisites for the parser.
        payload.put("timestamp", System.currentTimeMillis());
      }

      return ImmutableList.of(new JSONObject(payload));

    } catch (Exception e) {
      String message = "Unable to parse Message: " + originalString;
      _LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }

  }
}
