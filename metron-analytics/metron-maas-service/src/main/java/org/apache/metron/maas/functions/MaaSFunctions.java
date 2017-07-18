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
package org.apache.metron.maas.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.maas.config.Endpoint;
import org.apache.metron.maas.config.MaaSConfig;
import org.apache.metron.maas.config.ModelEndpoint;
import org.apache.metron.maas.discovery.ServiceDiscoverer;
import org.apache.metron.maas.util.ConfigUtil;
import org.apache.metron.maas.util.RESTUtil;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaaSFunctions {
 protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static class ModelCacheKey {
    String name;
    String version;
    String method;
    Map<String, String> args;
    public ModelCacheKey(String name, String version, String method, Map<String, String> args) {
      this.name = name;
      this.version = version;
      this.method = method;
      this.args = args;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ModelCacheKey that = (ModelCacheKey) o;

      if (name != null ? !name.equals(that.name) : that.name != null) return false;
      if (version != null ? !version.equals(that.version) : that.version != null) return false;
      if (method != null ? !method.equals(that.method) : that.method != null) return false;
      return args != null ? args.equals(that.args) : that.args == null;

    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (version != null ? version.hashCode() : 0);
      result = 31 * result + (method != null ? method.hashCode() : 0);
      result = 31 * result + (args != null ? args.hashCode() : 0);
      return result;
    }
  }

  @Stellar(name="MODEL_APPLY"
          , namespace="MAAS"
          , description = "Returns the output of a model deployed via Model as a Service. NOTE: Results are cached locally for 10 minutes."
          , params = { "endpoint - A map containing the name, version, and url for the REST endpoint"
                     , "function - The optional endpoint path; default is 'apply'"
                     , "model_args - A Dictionary of arguments for the model (these become request params)"
                     }
          , returns = "The output of the model deployed as a REST endpoint in Map form.  Assumes REST endpoint returns a JSON Map."
          )
  public static class ModelApply implements StellarFunction {
    private boolean isInitialized = false;
    private ServiceDiscoverer discoverer;
    private Cache<ModelCacheKey, Map<String, Object> > resultCache;
    public ModelApply() {
      resultCache = CacheBuilder.newBuilder()
                            .concurrencyLevel(4)
                            .weakKeys()
                            .maximumSize(100000)
                            .expireAfterWrite(10, TimeUnit.MINUTES)
                            .build();
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 2) {
        throw new ParseException("Unable to execute model_apply. " +
                                 "Expected arguments: endpoint_map:map, " +
                                 " [endpoint method:string], model_args:map"
                                 );
      }
      if(!isInitialized) {
        return null;
      }
      int i = 0;
      if(args.size() == 0) {
        return null;
      }
      Object endpointObj = args.get(i++);
      Map endpoint = null;
      String modelName;
      String modelVersion;
      String modelUrl;
      if(endpointObj instanceof Map) {
        endpoint = (Map)endpointObj;
        modelName = endpoint.get("name") + "";
        modelVersion = endpoint.get("version") + "";
        modelUrl = endpoint.get("url") + "";
      }
      else {
        return null;
      }
      String modelFunction = "apply";
      Map<String, String> modelArgs = new HashMap<>();
      if(args.get(i) instanceof String) {
        String func = (String)args.get(i);
        if(endpoint.containsKey("endpoint:" + func)) {
          modelFunction = "" + endpoint.get("endpoint:" + func);
        }
        else {
          modelFunction = func;
        }
        i++;
      }

      if(args.get(i) instanceof Map) {
        if(endpoint.containsKey("endpoint:apply")) {
          modelFunction = "" + endpoint.get("endpoint:apply");
        }
        modelArgs = (Map)args.get(i);
      }
      if( modelName == null
       || modelVersion == null
       || modelFunction == null
        ) {
        return null;
      }
      ModelCacheKey cacheKey = new ModelCacheKey(modelName, modelVersion, modelFunction, modelArgs);
      Map<String, Object> ret = resultCache.getIfPresent(cacheKey);
      if(ret != null) {
        return ret;
      }
      else {
        String url = modelUrl;
        if (url.endsWith("/")) {
          url = url.substring(0, url.length() - 1);
        }
        if (modelFunction.startsWith("/")) {
          modelFunction = modelFunction.substring(1);
        }
        try {
          URL u = new URL(url + "/" + modelFunction);

          String results = RESTUtil.INSTANCE.getRESTJSONResults(u, modelArgs);
          ret = JSONUtils.INSTANCE.load(results, new TypeReference<Map<String, Object>>() {
          });
          resultCache.put(cacheKey, ret);
          return ret;
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
          if (discoverer != null) {
            try {
              URL u = new URL(modelUrl);
              discoverer.blacklist(u);
            } catch (MalformedURLException e1) {
            }
          }
        }
      }
      return null;
    }

    @Override
    public synchronized void initialize(Context context) {

      try {
        Optional<ServiceDiscoverer> discovererOpt = (Optional) (context.getCapability(Context.Capabilities.SERVICE_DISCOVERER));
        if (discovererOpt.isPresent()) {
          discoverer = discovererOpt.get();
        }
        else {
          Optional<Object> clientOptional = context.getCapability(Context.Capabilities.ZOOKEEPER_CLIENT);
          CuratorFramework client = null;
          if (clientOptional.isPresent() && clientOptional.get() instanceof CuratorFramework) {
            client = (CuratorFramework) clientOptional.get();
          } else {
            throw new IllegalStateException("Unable to initialize function: Cannot find zookeeper client.");
          }
          discoverer = createDiscoverer(client);
        }
      }
      catch(Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
      finally {
        //We always want to set initialize to true because we don't want to keep trying to initialize over and over
        isInitialized = true;
      }
    }

    @Override
    public boolean isInitialized() {
      return isInitialized;
    }
  }

  private static ServiceDiscoverer createDiscoverer(CuratorFramework client) throws Exception {
    MaaSConfig config = ConfigUtil.INSTANCE.read(client, "/metron/maas/config", new MaaSConfig(), MaaSConfig.class);
    ServiceDiscoverer discoverer = new ServiceDiscoverer(client, config.getServiceRoot());
    discoverer.start();
    return discoverer;
  }

  @Stellar(name="GET_ENDPOINT"
          , namespace="MAAS"
          , description="Inspects ZooKeeper and returns a map containing the name, version and url for the model referred to by the input parameters."
          , params = {
                      "model_name - The name of the model"
                     ,"model_version - The optional version of the model.  If the model version is not specified, the most current version is used."
                     }
          , returns = "A map containing the name, version, and url for the REST endpoint (fields named name, version and url).  " +
                      "Note that the output of this function is suitable for input into the first argument of MAAS_MODEL_APPLY."
          )
  public static class GetEndpoint implements StellarFunction {
    ServiceDiscoverer discoverer;
    private boolean isInitialized = false;
    private boolean isValidState = false;

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(!isValidState) {
        LOG.error("Invalid state: Unable to find ServiceDiscoverer service.");
        return null;
      }
      String modelName = null;
      String modelVersion = null;
      if(args.size() >= 1) {
        modelName = args.get(0).toString();
      }
      if(args.size() >= 2)
      {
        modelVersion = args.get(1).toString();
      }
      if(modelName == null) {
        return null;
      }
      try {
        ModelEndpoint ep = null;
        if (modelVersion == null) {
          ep = discoverer.getEndpoint(modelName);
        } else {
          ep = discoverer.getEndpoint(modelName, modelVersion);
        }
        return ep == null ? null : endpointToMap(ep.getName(), ep.getVersion(), ep.getEndpoint());
      }
      catch(Exception ex) {
        LOG.error("Unable to discover endpoint: {}", ex.getMessage(), ex);
        return null;
      }
    }

    public static Map<String, String> endpointToMap(String name, String version, Endpoint ep) {
      Map<String, String> ret = new HashMap<>();
      ret.put("url", ep.getUrl());
      ret.put("name", name);
      ret.put("version", version);
      for(Map.Entry<String, String> kv : ep.getFunctions().entrySet()) {
        ret.put("endpoint:" + kv.getKey(), kv.getValue());
      }
      return ret;
    }

    @Override
    public synchronized void initialize(Context context) {
      try {
        Optional<Object> clientOptional = context.getCapability(Context.Capabilities.ZOOKEEPER_CLIENT);
        CuratorFramework client = null;
        if (clientOptional.isPresent() && clientOptional.get() instanceof CuratorFramework) {
          client = (CuratorFramework) clientOptional.get();
        } else {
          throw new IllegalStateException("Unable to initialize function: Cannot find zookeeper client.");
        }
        try {
          discoverer = createDiscoverer(client);
          context.addCapability(Context.Capabilities.SERVICE_DISCOVERER, () -> discoverer);
          isValidState = true;
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
          throw new IllegalStateException("Unable to initialize MAAS_GET_ENDPOINT", e);
        }
      }
      finally {
        isInitialized = true;
      }
    }

    @Override
    public boolean isInitialized() {
      return isInitialized;
    }
  }
}
