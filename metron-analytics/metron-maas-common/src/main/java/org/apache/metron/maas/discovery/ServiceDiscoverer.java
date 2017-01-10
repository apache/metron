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

import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Iterables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.metron.maas.config.Model;
import org.apache.metron.maas.config.ModelEndpoint;
import org.apache.zookeeper.data.Stat;

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

/**
 * The discovery class for services registered by MaaS
 */
public class ServiceDiscoverer implements Closeable{
  private static final Log LOG = LogFactory.getLog(ServiceDiscoverer.class);
  private static final int BLACKLIST_EXPIRATION_DEFAULT = 10;
  private TreeCache cache;
  private ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private ServiceDiscovery<ModelEndpoint> serviceDiscovery;
  private Map<Model, List<ModelEndpoint>> state = new HashMap<>();
  private Map<String, ServiceInstance<ModelEndpoint>> containerToEndpoint = new HashMap<>();
  private Map<String, String> modelToCurrentVersion = new HashMap<>();
  private Cache<URL, Boolean> blacklist;
  public ServiceDiscoverer(CuratorFramework client, String root) {
    this(client, root, BLACKLIST_EXPIRATION_DEFAULT);
  }

  /**
   * This class listens to zookeeper and updates its internal state when new model endpoints are
   * added via the MaaS service.
   *
   * @param client The zookeeper client
   * @param root The discovery root
   * @param blacklistExpirationMin The amount of time (in minutes) that a blacklisted URL can be held in the blacklist before retrying.
   */
  public ServiceDiscoverer(CuratorFramework client, String root, int blacklistExpirationMin) {
    blacklist = CacheBuilder.newBuilder()
                            .concurrencyLevel(4)
                            .weakKeys()
                            .expireAfterWrite(blacklistExpirationMin, TimeUnit.MINUTES)
                            .build();
    try {
      Stat exists = client.checkExists().forPath(root);
      if(exists == null) {
        client.create().creatingParentsIfNeeded().forPath(root);
      }
    } catch (Exception e) {
      LOG.error("Unable to create path: " + e.getMessage(), e);
    }
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

  /**
   * Reset the state to empty.
   */
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

  /**
   * Get the underlying Curator ServiceDiscovery implementation.
   * @return
   */
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
          if(LOG.isDebugEnabled()) {
            LOG.debug("Found model endpoint " + ep);
          }
          //initialize to the existing current version, defaulting to this version
          String currentVersion = modelToVersion.getOrDefault(ep.getName(), ep.getVersion());
          //if the version for this endpont is greater than the current version, then use this one
          //otherwise use the version we know about.
          //essentially it's equivalent to currentVersion = max(ep.getVersion(), currentVersion)
          currentVersion = currentVersion.compareTo(ep.getVersion()) < 0
                            ? ep.getVersion()
                            : currentVersion
                  ;
          modelToVersion.put( ep.getName()
                            , currentVersion
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
      try {
        this.modelToCurrentVersion = modelToVersion;
        this.state = state;
        this.containerToEndpoint = containerToEndpoint;
        if(LOG.isDebugEnabled()) {
          LOG.debug("Containers found: " + containerToEndpoint);
        }
      }
      finally {
        rwLock.writeLock().unlock();
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    } finally {
    }

  }


  /**
   * Start the discovery service
   */
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

  /**
   * Unregister a service based on its container ID.
   * @param containerIdRaw
   */
  public void unregisterByContainer(String containerIdRaw) {
    rwLock.readLock().lock();
    try {
      String containerId = containerIdRaw;
      //if(containerIdRaw.contains("_")) {
      //  containerId = "" + Long.parseLong(Iterables.getLast(Splitter.on("_").split(containerIdRaw), null));
      //}
      ServiceInstance<ModelEndpoint> ep = containerToEndpoint.get(containerId);
      if(ep != null) {
        serviceDiscovery.unregisterService(ep);
      }
      else {
        LOG.warn("Unable to find registered model associated with container " + containerId);
        throw new IllegalStateException("Unable.");
      }
    } catch (Exception e) {
      LOG.error("Unable to unregister container " + containerIdRaw + " due to: " + e.getMessage(), e);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Retrieve the endpoints for a given model.  A model may or may not have a version associated with it.
   * If the version is missing, then all endpoints are returned associated with that model name.
   * @param model
   * @return
   */
  public List<ModelEndpoint> getEndpoints(Model model) {
    rwLock.readLock().lock();
    try {
      return state.getOrDefault(model, new ArrayList<>());
    }
    finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Blacklist a model endpoint
   * @param endpoint
   */
  public void blacklist(ModelEndpoint endpoint) {
    blacklist(toUrl(endpoint.getEndpoint().getUrl()));
  }

  /**
   * Blacklist a model URL.
   * @param url
   */
  public void blacklist(URL url) {
    rwLock.writeLock().lock();
    try {
      blacklist.put(url, true);
    }
    finally {
      rwLock.writeLock().unlock();
    }
  }

  public ModelEndpoint getEndpoint(String modelName) {
    String version = null;
    rwLock.readLock().lock();
    try {
      version = modelToCurrentVersion.get(modelName);
    }
    finally {
      rwLock.readLock().unlock();
    }
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

  /**
   * Retrieve an endpoint based on name and version of a model.
   * This will retrieve one endpoint at random for a given model.
   * @param modelName
   * @param modelVersion can be null
   * @return
   */
  public ModelEndpoint getEndpoint(String modelName, String modelVersion) {
    return getEndpoint(new Model(modelName, modelVersion));
  }

  /**
   * Retrieve an endpoint at random of a given model
   * @param model
   * @return
   */
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
            /*
             If an exception happens on an attempt then we move on.
             Frankly this is an excess of caution since we parse the
             URLs in the Runner before they go into zookeeper, so they are valid.
             */
          }
        }
      }
      return ret;
    }
    finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * List all endpoints for a given model.
   *
   * @param model
   * @return
   */
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

  /**
   * Close the discoverer service
   */
  @Override
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
