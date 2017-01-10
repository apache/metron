package org.apache.metron.common.bolt;

import org.apache.log4j.Logger;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.IndexingConfigurations;

import java.io.IOException;

public abstract class ConfiguredIndexingBolt extends ConfiguredBolt<IndexingConfigurations>{

  private static final Logger LOG = Logger.getLogger(ConfiguredIndexingBolt.class);

  public ConfiguredIndexingBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @Override
  protected IndexingConfigurations defaultConfigurations() {
    return new IndexingConfigurations();
  }

  @Override
  public void loadConfig() {
    try {
      ConfigurationsUtils.updateSensorIndexingConfigsFromZookeeper(getConfigurations(), client);
    } catch (Exception e) {
      LOG.warn("Unable to load configs from zookeeper, but the cache should load lazily...");
    }
  }

  @Override
  public void updateConfig(String path, byte[] data) throws IOException {
    if (data.length != 0) {
      String name = path.substring(path.lastIndexOf("/") + 1);
      if (path.startsWith(ConfigurationType.INDEXING.getZookeeperRoot())) {
        getConfigurations().updateSensorIndexingConfig(name, data);
        reloadCallback(name, ConfigurationType.INDEXING);
      } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
        getConfigurations().updateGlobalConfig(data);
        reloadCallback(name, ConfigurationType.GLOBAL);
      }
    }
  }
}
