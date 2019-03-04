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

package org.apache.metron.rest.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;

/**
 * Logs information about Http responses including status code and payload.
 */
@Component
public class ResponseLoggingFilter implements Filter {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    if (LOG.isDebugEnabled()) {
      response = new ContentCachingResponseWrapper((HttpServletResponse) response);
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (LOG.isDebugEnabled()) {
        int status = -1;
        String responseBody = "";
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
          status = wrapper.getStatus();
          responseBody = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
          // try to pretty print
          try {
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(responseBody, Object.class));
          } catch(Exception e) {
            // if pretty printing fails or response is not json, just move on
          }
          // need to copy the response back since we're reading it here
          wrapper.copyBodyToResponse();
        }
        LOG.debug(String.format("response: status=%d;payload=%s", status, responseBody));
      }
    }
  }

  @Override
  public void destroy() {
  }
}
