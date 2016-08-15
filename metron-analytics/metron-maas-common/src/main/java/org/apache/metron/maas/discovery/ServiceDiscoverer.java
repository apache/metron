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
package org.apache.metron.maas.discovery;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.metron.maas.config.Model;
import org.apache.metron.maas.config.ModelEndpoint;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServiceDiscoverer implements Closeable{
  private static final Log LOG = LogFactory.getLog(ServiceDiscoverer.class);
  private TreeCache cache;
  private ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private ServiceDiscovery<ModelEndpoint> serviceDiscovery;
  private Map<Model, List<ModelEndpoint>> state = new HashMap<>();
  private Map<String, ServiceInstance<ModelEndpoint>> containerToEndpoint = new HashMap<>();
  private Map<String, String> modelToCurrentVersion = new HashMap<>();
  private Cache<URL, Boolean> blacklist;

  public ServiceDiscoverer(CuratorFramework client, String root) {
    blacklist = CacheBuilder.newBuilder()
                            .concurrencyLevel(4)
                            .weakKeys()
                            .expireAfterWrite(10, TimeUnit.MINUTES)
                            .build();

    JsonInstanceSerializer<ModelEndpoint> serializer = new JsonInstanceSerializer<>(ModelEndpoint.class);
    serviceDiscovery = ServiceDiscoveryBuilder.builder(ModelEndpoint.class)
                .client(client)
                .basePath(root)
                .serializer(serializer)
                .build();
    cache = new TreeCache(client, root);
    cache.getListenable().addListener((client1, event) -> {
      updateState();
    });
    updateState();
  }

  public void resetState() {
    rwLock.readLock().lock();
    ServiceInstance<ModelEndpoint> ep = null;
    try {
      for(Map.Entry<String, ServiceInstance<ModelEndpoint>> kv : containerToEndpoint.entrySet()) {
        ep = kv.getValue();
        serviceDiscovery.unregisterService(ep);
      }
    } catch (Exception e) {
      LOG.error("Unable to unregister endpoint " + ep.getPayload(), e);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  public ServiceDiscovery<ModelEndpoint> getServiceDiscovery() {
    return serviceDiscovery;
  }

  private void updateState() {
    Map<Model, List<ModelEndpoint>> state = new HashMap<>();
    Map<String, String> modelToVersion = new HashMap<>();
    Map<String, ServiceInstance<ModelEndpoint>> containerToEndpoint = new HashMap<>();
    try {
      for(String name : serviceDiscovery.queryForNames()) {
        for(ServiceInstance<ModelEndpoint> endpoint: serviceDiscovery.queryForInstances(name)) {
          ModelEndpoint ep = endpoint.getPayload();
          LOG.info("Found model endpoint " + ep);
          String currentVersion = modelToVersion.getOrDefault(ep.getName(), ep.getVersion());
          modelToVersion.put( ep.getName()
                            , currentVersion.compareTo(ep.getVersion()) < 0
                            ? ep.getVersion()
                            : currentVersion
                            );
          containerToEndpoint.put(ep.getContainerId(), endpoint);
          Model model = new Model(ep.getName(), ep.getVersion());
          List<ModelEndpoint> endpoints = state.get(model);
          if(endpoints == null) {
            endpoints = new ArrayList<>();
            state.put(model, endpoints);
          }
          endpoints.add(ep);
        }
      }
      rwLock.writeLock().lock();
      this.modelToCurrentVersion = modelToVersion;
      this.state = state;
      this.containerToEndpoint = containerToEndpoint;
      rwLock.writeLock().unlock();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    } finally {
    }

  }


  public void start() {
    try {
      serviceDiscovery.start();
      cache.start();
      updateState();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new IllegalStateException("Unable to start", e);
    }
  }

  public void unregisterByContainer(String containerId) {
    rwLock.readLock().lock();
    try {
      ServiceInstance<ModelEndpoint> ep = containerToEndpoint.get(containerId);
      if(ep != null) {
        serviceDiscovery.unregisterService(ep);
        LOG.info("Unregistered endpoint " + ep.getPayload());
      }
      else {
        LOG.warn("Unable to find registered model associated with container " + containerId);
      }
    } catch (Exception e) {
      LOG.error("Unable to unregister container " + containerId + " due to: " + e.getMessage(), e);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  public List<ModelEndpoint> getEndpoints(Model model) {
    rwLock.readLock().lock();
    try {
      return state.getOrDefault(model, new ArrayList<>());
    }
    finally {
      rwLock.readLock().unlock();
    }
  }

  public void blacklist(ModelEndpoint endpoint) {
    blacklist(toUrl(endpoint.getEndpoint().getUrl()));
  }

  public void blacklist(URL url) {
    rwLock.writeLock().lock();
    blacklist.put(url, true);
    rwLock.writeLock().unlock();
  }

  public ModelEndpoint getEndpoint(String modelName) {
    String version = null;
    rwLock.readLock().lock();
    version = modelToCurrentVersion.get(modelName);
    rwLock.readLock().unlock();
    if(version == null) {
      throw new IllegalStateException("Unable to find version for " + modelName);
    }
    return getEndpoint(modelName, version);
  }

  private static URL toUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Endpoint does not refer to an actual URL");
    }
  }

  public ModelEndpoint getEndpoint(String modelName, String modelVersion) {
    return getEndpoint(new Model(modelName, modelVersion));
  }
  public ModelEndpoint getEndpoint(Model model) {
    rwLock.readLock().lock();
    try {
      List<ModelEndpoint> endpoints = state.get(model);
      ModelEndpoint ret = null;
      if(endpoints != null) {
        for(int j = 0;j < 10;++j) {
          int i = ThreadLocalRandom.current().nextInt(endpoints.size());
          ret = endpoints.get(i);
          try {
            if (blacklist.asMap().containsKey(toUrl(ret.getEndpoint().getUrl()))) {
              continue;
            }
            else {
              return ret;
            }
          }
          catch(IllegalStateException ise) {

          }
        }
      }
      return ret;
    }
    finally {
      rwLock.readLock().unlock();
    }
  }

  public Map<Model, List<ModelEndpoint>> listEndpoints(Model model) {
    Map<Model, List<ModelEndpoint>> ret = new HashMap<>();
    rwLock.readLock().lock();
    try {
      Query query = new Query(model);
      for(Map.Entry<Model, List<ModelEndpoint>> kv : state.entrySet()) {
        if(query.match(kv.getKey())) {
          ret.put(kv.getKey(), kv.getValue());
        }
      }
      return ret;
    }
    finally {
      rwLock.readLock().unlock();
    }
  }

  public void close() {
    if(cache != null) {
      CloseableUtils.closeQuietly(cache);
    }
    if(serviceDiscovery != null) {
      CloseableUtils.closeQuietly(serviceDiscovery);
    }

  }

  private static class Query {
    Model model;
    public Query(Model model) {
      this.model = model;
    }

    public boolean match(Model m) {
      boolean isNameMatch = ((model.getName() != null && model.getName().equals(m.getName())) || model.getName() == null);
      if(!isNameMatch) {
        return false;
      }
      boolean isVersionMatch = (model.getVersion() != null && model.getVersion().equals(m.getVersion())) || model.getVersion() == null;
      if(!isVersionMatch) {
        return false;
      }

      return true;
    }
  }

}
