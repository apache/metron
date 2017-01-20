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
package org.apache.metron.rest.utils;

import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestControllerInfo {

  public class Response {

    private String message;
    private int code;

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public int getCode() {
      return code;
    }

    public void setCode(int code) {
      this.code = code;
    }
  }

  private String path;
  private String description;
  private RequestMethod method;
  private List<Response> responses = new ArrayList<>();
  private Map<String, String> parameterDescriptions = new HashMap<>();

  public RestControllerInfo() {}

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public RequestMethod getMethod() {
    return method;
  }

  public void setMethod(RequestMethod method) {
    this.method = method;
  }

  public List<Response> getResponses() {
    return responses;
  }

  public void addResponse(String message, int code) {
    Response response = new Response();
    response.setMessage(message);
    response.setCode(code);
    this.responses.add(response);
  }

  public Map<String, String> getParameterDescriptions() {
    return parameterDescriptions;
  }

  public void addParameterDescription(String name, String description) {
    parameterDescriptions.put(name, description);
  }
}


