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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.reflections.Reflections;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ReadMeUtils {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("README output path must be passed in as first argument");
    }

    Reflections reflections = new Reflections("org.apache.metron.rest.controller");
    List<RestControllerInfo> endpoints = new ArrayList<>();
    Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(RestController.class);
    for(Class<?> controller: controllers) {
      RequestMapping controllerRequestMapping = controller.getAnnotation(RequestMapping.class);
      String pathPrefix;
      if (controllerRequestMapping == null) {
        pathPrefix = "";
      } else {
        pathPrefix = controllerRequestMapping.value()[0];
      }
      for(Method method: controller.getDeclaredMethods()) {
        RestControllerInfo restControllerInfo = new RestControllerInfo();
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
          throw new Exception(String.format("@RequestMapping annotation missing from method %s.%s. Every controller method must have a @RequestMapping annotation.", controller.getSimpleName(), method.getName()));
        }
        ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
        if (apiOperation == null) {
          throw new Exception(String.format("@ApiOperation annotation missing from method %s.%s. Every controller method must have an @ApiOperation annotation.", controller.getSimpleName(), method.getName()));
        }
        ApiResponse[] apiResponseArray;
        ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
        if (apiResponses == null) {
          ApiResponse apiResponse = method.getAnnotation(ApiResponse.class);
          if (apiResponse == null) {
            throw new Exception(String.format("@ApiResponses or @ApiResponse annotation missing from method %s.%s. Every controller method must have an @ApiResponses or @ApiResponse annotation.", controller.getSimpleName(), method.getName()));
          } else {
            apiResponseArray = new ApiResponse[]{ apiResponse };
          }
        } else {
          apiResponseArray = apiResponses.value();
        }
        String[] requestMappingValue = requestMapping.value();
        if (requestMappingValue.length == 0) {
          restControllerInfo.setPath(pathPrefix);
        } else {
          restControllerInfo.setPath(pathPrefix + requestMappingValue[0]);
        }
        restControllerInfo.setDescription(apiOperation.value());
        RequestMethod requestMethod;
        if (requestMapping.method().length == 0) {
          requestMethod = RequestMethod.GET;
        } else {
          requestMethod = requestMapping.method()[0];
        }
        restControllerInfo.setMethod(requestMethod);
        for (ApiResponse apiResponse: apiResponseArray) {
          restControllerInfo.addResponse(apiResponse.message(), apiResponse.code());
        }
        for(Parameter parameter: method.getParameters()) {
          if (!parameter.getType().equals(Principal.class)) {
            ApiParam apiParam = parameter.getAnnotation(ApiParam.class);
            if (apiParam == null) {
              throw new Exception(String.format("@ApiParam annotation missing from parameter %s.%s.%s. Every controller method parameter must have an @ApiParam annotation.", controller.getSimpleName(), method.getName(), parameter.getName()));
            }
            restControllerInfo.addParameterDescription(apiParam.name(), apiParam.value());
          }
        }
        endpoints.add(restControllerInfo);
      }
    }
    Collections.sort(endpoints, (o1, o2) -> o1.getPath().compareTo(o2.getPath()));

    Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    Velocity.init();

    VelocityContext context = new VelocityContext();
    context.put( "endpoints", endpoints );

    Template template = Velocity.getTemplate("README.vm");
    FileWriter fileWriter = new FileWriter(args[0]);
    template.merge( context, fileWriter );
    fileWriter.close();
  }
}
