package org.apache.metron.enrichment.adapters.maxmind;/*
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

import com.maxmind.geoip2.DatabaseReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MaxMindDatabase {
  Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  String EXTENSION_MMDB = ".mmdb";
  String EXTENSION_TAR_GZ = ".tar.gz";
  String EXTENSION_MMDB_GZ = ".mmdb.gz";

  /**
   * Retrieves the configuration key that holds the HDFS database file location
   * @return The configuration key
   */
  String getHdfsFileConfig();

  /**
   * Retrieves the default HDFS database file location
   * @return The HDFS database file location
   */
  String getHdfsFileDefault();

  /**
   * Locks any underlying resources to ensure they are smoothly updated without disruption.
   * Any callers implementing an update() function should lock during the update to ensure uninterrupted querying.
   */
  void lockIfNecessary();

  /**
   * Unlocks any underlying resources to ensure the lock is released after an update.
   * Any callers implementing an update() function should ensure they've unlocked post update.
   */
  void unlockIfNecessary();

  /**
   * Gets the appropriate database reader for the underlying database that's been loaded
   * @return The DatabaseReader for the MaxMind database.
   */
  DatabaseReader getReader();
  void setReader(DatabaseReader reader);

  /**
   * Updates the database file, if the configuration points to a new file.
   * Implementations may need to be synchronized to avoid issues querying during updates.
   *
   * @param globalConfig The global configuration that will be used to determine if an update is necessary.
   */
  void updateIfNecessary(Map<String, Object> globalConfig);

  /**
   * Update the database being queried to one backed by the provided HDFS file.
   * Access to the database should be guarded by read locks to avoid disruption while updates are occurring.
   * @param hdfsFile The HDFS file path to be used for new queries.
   */
  default void update(String hdfsFile) {
    // If nothing is set (or it's been unset, use the defaults)
    if (hdfsFile == null || hdfsFile.isEmpty()) {
      LOG.debug("Using default for {}: {}", getHdfsFileConfig(), getHdfsFileDefault());
      hdfsFile = getHdfsFileDefault();
    }

    FileSystem fs = MaxMindDbUtilities.getFileSystem();

    if (hdfsFile.endsWith(MaxMindDatabase.EXTENSION_MMDB)) {
      lockIfNecessary();
      try (BufferedInputStream is = new BufferedInputStream(fs.open(new Path(hdfsFile)))) {
        setReader(MaxMindDbUtilities.readNewDatabase(getReader(), hdfsFile, is));
      } catch (IOException e) {
        MaxMindDbUtilities.handleDatabaseIOException(hdfsFile, e);
      } finally {
        unlockIfNecessary();
      }
    } else if (hdfsFile.endsWith(MaxMindDatabase.EXTENSION_MMDB_GZ)) {
      lockIfNecessary();
      try (GZIPInputStream is = new GZIPInputStream(fs.open(new Path(hdfsFile)))) {
        setReader(MaxMindDbUtilities.readNewDatabase(getReader(), hdfsFile, is));
      } catch (IOException e) {
        MaxMindDbUtilities.handleDatabaseIOException(hdfsFile, e);
      } finally {
        unlockIfNecessary();
      }
    } else if (hdfsFile.endsWith(MaxMindDatabase.EXTENSION_TAR_GZ)) {
      lockIfNecessary();
      try (TarArchiveInputStream is = new TarArchiveInputStream(
          new GZIPInputStream(fs.open(new Path(hdfsFile))))) {
        // Need to find the mmdb entry.
        TarArchiveEntry entry = is.getNextTarEntry();
        while (entry != null) {
          if (entry.isFile() && entry.getName().endsWith(MaxMindDatabase.EXTENSION_MMDB)) {
            try(InputStream mmdb = new BufferedInputStream(is))
            { // Read directly from tarInput
              setReader(MaxMindDbUtilities.readNewDatabase(getReader(), hdfsFile, mmdb));
              break; // Don't care about the other entries, leave immediately
            }
          }
          entry = is.getNextTarEntry();
        }
      } catch (IOException e) {
        MaxMindDbUtilities.handleDatabaseIOException(hdfsFile, e);
      } finally {
        unlockIfNecessary();
      }
    }
  }
}
