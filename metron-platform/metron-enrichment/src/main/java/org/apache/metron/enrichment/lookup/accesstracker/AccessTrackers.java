package org.apache.metron.enrichment.lookup.accesstracker;


import org.apache.metron.hbase.TableProvider;

import java.io.IOException;
import java.util.Map;

public enum AccessTrackers implements AccessTrackerCreator {
   NOOP((config, provider) -> new NoopAccessTracker())
  ,PERSISTENT_BLOOM( new PersistentBloomTrackerCreator());
  AccessTrackerCreator creator;
  AccessTrackers(AccessTrackerCreator creator) {
    this.creator = creator;
  }


  @Override
  public AccessTracker create(Map<String, Object> config, TableProvider provider) throws IOException {
    return creator.create(config, provider);
  }
}
