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
