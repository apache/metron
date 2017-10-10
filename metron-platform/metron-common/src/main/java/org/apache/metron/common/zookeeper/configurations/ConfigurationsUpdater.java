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
package org.apache.metron.common.zookeeper.configurations;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

public abstract class ConfigurationsUpdater<T extends Configurations> implements Serializable {
  protected static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Reloadable reloadable;
  private Supplier<T> configSupplier;
  public ConfigurationsUpdater(Reloadable reloadable
                              , Supplier<T> configSupplier
  )
  {
    this.reloadable = reloadable;
    this.configSupplier = configSupplier;
  }


  public abstract void update(CuratorFramework client, String path, byte[] data) throws IOException;
  public abstract void delete(CuratorFramework client, String path, byte[] data) throws IOException;

  public abstract Class<T> getConfigurationClass();
  public abstract void forceUpdate(CuratorFramework client);
  public abstract T defaultConfigurations();

  protected void reloadCallback(String name, ConfigurationType type) {
    reloadable.reloadCallback(name, type);
  }

  protected T getConfigurations() {
    return configSupplier.get();
  }

}
