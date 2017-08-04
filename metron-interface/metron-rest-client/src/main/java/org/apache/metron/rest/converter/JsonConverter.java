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

package org.apache.metron.rest.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.apache.metron.common.utils.JSONUtils;

@Converter
public class JsonConverter implements AttributeConverter<Object, String> {

  @Override
  public String convertToDatabaseColumn(Object savedSearches) {
    try {
      return JSONUtils.INSTANCE.toJSON(savedSearches, false);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Object convertToEntityAttribute(String savedSearches) {
    try {
      return JSONUtils.INSTANCE.load(savedSearches, Object.class);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
