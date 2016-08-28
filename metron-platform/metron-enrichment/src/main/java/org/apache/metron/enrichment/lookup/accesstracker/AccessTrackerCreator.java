package org.apache.metron.enrichment.lookup.accesstracker;

import org.apache.metron.hbase.TableProvider;

import java.io.IOException;
import java.util.Map;

public interface AccessTrackerCreator {
  public AccessTracker create(Map<String, Object> config, TableProvider provider) throws IOException;
}
