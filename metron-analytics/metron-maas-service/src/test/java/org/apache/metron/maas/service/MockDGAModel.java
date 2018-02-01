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
package org.apache.metron.maas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.metron.stellar.common.utils.JSONUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;

@Path("/")
public class MockDGAModel {
  private static HttpServer server;
  private Map<String, Boolean> isMalicious = ImmutableMap.of( "badguy.com", true
                                                           );

  @GET
  @Path("/apply")
  @Produces("application/json")
  public Response apply(@QueryParam("host") String host ) throws JsonProcessingException {
    Boolean b = isMalicious.get(host);
    boolean isMalicious = b != null && b;
    Map<String, Boolean> ret = new HashMap<String, Boolean>();
    ret.put("is_malicious", isMalicious );
    String resp = JSONUtils.INSTANCE.toJSON(ret, true);
    return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @ApplicationPath("rs")
  public static class ApplicationConfig extends Application {
    private final Set<Class<?>> classes;
    public ApplicationConfig() {
      HashSet<Class<?>> c = new HashSet<>();
      c.add(MockDGAModel.class);
      classes = Collections.unmodifiableSet(c);
    }
    @Override
    public Set<Class<?>> getClasses() {
      return classes;
    }
  }

  public static void start(int port) throws IOException {
    // Create an HTTP server listening at port
    URI uri = UriBuilder.fromUri("http://localhost/").port(port).build();
    server = HttpServer.create(new InetSocketAddress(uri.getPort()), 0);
    HttpHandler handler = RuntimeDelegate.getInstance().createEndpoint(new ApplicationConfig(), HttpHandler.class);
    server.createContext(uri.getPath(), handler);
    server.start();
  }

  public static void shutdown() {
    if(server != null) {
      server.stop(0);
    }
  }
}
