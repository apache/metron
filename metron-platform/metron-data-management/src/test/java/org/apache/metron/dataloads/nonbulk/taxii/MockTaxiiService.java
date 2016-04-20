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
package org.apache.metron.dataloads.nonbulk.taxii;


import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.FileUtils;
import org.apache.metron.test.utils.UnitTestHelper;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Path("/")
public class MockTaxiiService {
    static String discoveryMsg;
    static String pollMsg;
    static {
        try {
            String baseDir = UnitTestHelper.findDir("taxii-messages");
            discoveryMsg = FileUtils.readFileToString(new File(new File(baseDir), "message.discovery"));
            pollMsg= FileUtils.readFileToString(new File(new File(baseDir), "messages.poll"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read discovery message", e);
        }
    }


    @POST
    @Path("/taxii-discovery-service")
    public Response getDiscovery() {
        return Response.ok(discoveryMsg, MediaType.APPLICATION_XML_TYPE).header("x-taxii-content-type", "urn:taxii.mitre.org:message:xml:1.1").build();
    }
    @POST
    @Path("/taxii-data")
    public Response getData() {
        return Response.ok(pollMsg).type(MediaType.APPLICATION_XML_TYPE).header("x-taxii-content-type", "urn:taxii.mitre.org:message:xml:1.1").build();
    }

    @ApplicationPath("rs")
    public static class ApplicationConfig extends Application{
        private final Set<Class<?>> classes;
        public ApplicationConfig() {
            HashSet<Class<?>> c = new HashSet<>();
            c.add(MockTaxiiService.class);
            classes = Collections.unmodifiableSet(c);
        }
        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }
    }
    private static HttpServer server;
    public static void start(int port) throws IOException {
        // Create an HTTP server listening at port 8282
        URI uri = UriBuilder.fromUri("http://localhost/").port(port).build();
        server = HttpServer.create(new InetSocketAddress(uri.getPort()), 0);
        HttpHandler handler = RuntimeDelegate.getInstance().createEndpoint(new ApplicationConfig(), HttpHandler.class);
        server.createContext(uri.getPath(), handler);
        discoveryMsg = discoveryMsg.replaceAll("PORT", "" + uri.getPort());
        server.start();
    }

    public static void shutdown() {
        if(server != null) {
            server.stop(0);
        }
    }
}
