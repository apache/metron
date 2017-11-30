package org.apache.metron.common.configuration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.Constants;

public interface ConfigurationOperations {
  String getTypeName();
  String getDirectory();
  default String getZookeeperRoot() {
    return Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + getTypeName();
  }
  Object deserialize(String s);
  void writeSensorConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception;
}
