package org.apache.hadoop.hbase.regionserver.compactions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.regionserver.compactions.RatioBasedCompactionPolicy;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.regionserver.StoreConfigInformation;
import org.apache.hadoop.hbase.regionserver.StoreFile;

public class OnlyDeleteExpiredFilesCompactionPolicy extends RatioBasedCompactionPolicy {
  private static final Log LOG = LogFactory.getLog(OnlyDeleteExpiredFilesCompactionPolicy.class);

  /**
   * Constructor.
   * 
   * @param conf
   *          The Conf.
   * @param storeConfigInfo
   *          Info about the store.
   */
  public OnlyDeleteExpiredFilesCompactionPolicy(final Configuration conf, final StoreConfigInformation storeConfigInfo) {
    super(conf, storeConfigInfo);
  }

  @Override
  final ArrayList<StoreFile> applyCompactionPolicy(final ArrayList<StoreFile> candidates, final boolean mayUseOffPeak,
      final boolean mayBeStuck) throws IOException {
    LOG.info("Sending empty list for compaction to avoid compaction and do only deletes of files older than TTL");

    return new ArrayList<StoreFile>();
  }

}
