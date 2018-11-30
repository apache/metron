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

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface GeoLiteDatabase {
  Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  InetAddressValidator ipvalidator = new InetAddressValidator();
  String EXTENSION_MMDB = ".mmdb";
  String EXTENSION_TAR_GZ = ".tar.gz";
  String EXTENSION_MMDB_GZ = ".mmdb.gz";

  String getHdfsFileConfig();
  String getHdfsFileDefault();
  void lockIfNecessary();
  void unlockIfNecessary();
  DatabaseReader getReader();
  void setReader(DatabaseReader reader);

  /**
   * Updates the database file, if the configuration points to a new file.
   *
   * @param globalConfig
   */
  void updateIfNecessary(Map<String, Object> globalConfig);

//  void update(String hdfsFile);

  default void update(String hdfsFile) {
    // If nothing is set (or it's been unset, use the defaults)
    if (hdfsFile == null || hdfsFile.isEmpty()) {
      LOG.debug("[Metron] Using default for {}: {}", getHdfsFileConfig(), getHdfsFileDefault());
      hdfsFile = getHdfsFileDefault();
    }

    FileSystem fs = getFileSystem();

    if (hdfsFile.endsWith(GeoLiteDatabase.EXTENSION_MMDB)) {
      lockIfNecessary();
      try (BufferedInputStream is = new BufferedInputStream(fs.open(new Path(hdfsFile)))) {
        setReader(readNewDatabase(getReader(), hdfsFile, is));
      } catch (IOException e) {
        handleDatabaseIOException(hdfsFile, e);
      } finally {
        unlockIfNecessary();
      }
    } else if (hdfsFile.endsWith(GeoLiteDatabase.EXTENSION_MMDB_GZ)) {
      lockIfNecessary();
      try (GZIPInputStream is = new GZIPInputStream(fs.open(new Path(hdfsFile)))) {
        setReader(readNewDatabase(getReader(), hdfsFile, is));
      } catch (IOException e) {
        handleDatabaseIOException(hdfsFile, e);
      } finally {
        unlockIfNecessary();
      }
    } else if (hdfsFile.endsWith(GeoLiteDatabase.EXTENSION_TAR_GZ)) {
      lockIfNecessary();
      try (TarArchiveInputStream is = new TarArchiveInputStream(
          new GZIPInputStream(fs.open(new Path(hdfsFile))))) {
        // Need to find the mmdb entry.
        TarArchiveEntry entry = is.getNextTarEntry();
        while (entry != null) {
          if (entry.isFile() && entry.getName().endsWith(GeoLiteDatabase.EXTENSION_MMDB)) {
            try(InputStream mmdb = new BufferedInputStream(is))
            { // Read directly from tarInput
              setReader(readNewDatabase(getReader(), hdfsFile, mmdb));
              break; // Don't care about the other entries, leave immediately
            }
          }
          entry = is.getNextTarEntry();
        }
      } catch (IOException e) {
        handleDatabaseIOException(hdfsFile, e);
      } finally {
        unlockIfNecessary();
      }
    }
  }

  default void handleDatabaseIOException(String hdfsFile, IOException e) {
    LOG.error("[Metron] Unable to open new database file {}", hdfsFile, e);
    throw new IllegalStateException("[Metron] Unable to update MaxMind database");
  }

  default DatabaseReader readNewDatabase(DatabaseReader reader, String hdfsFile, InputStream is) throws IOException {
    LOG.info("[Metron] Update to GeoIP data started with {}", hdfsFile);
    // InputStream based DatabaseReaders are always in memory.
    DatabaseReader newReader = new DatabaseReader.Builder(is).withCache(new CHMCache()).build();
    // If we've never set a reader, don't close the old one
    if (reader != null) {
      reader.close();
    }
    LOG.info("[Metron] Finished update to GeoIP data started with {}", hdfsFile);
    return newReader;
  }

  default FileSystem getFileSystem() {
    FileSystem fs;
    try {
      fs = FileSystem.get(new Configuration());
    } catch (IOException e) {
      LOG.error("[Metron] Unable to retrieve get HDFS FileSystem");
      throw new IllegalStateException("[Metron] Unable to get HDFS FileSystem");
    }
    return fs;
  }

  default String convertNullToEmptyString(Object raw) {
    return raw == null ? "" : String.valueOf(raw);
  }

  default boolean invalidIp(String ip) {
    LOG.trace("[Metron] Called validateIp({})", ip);
    InetAddress addr;
    try {
      addr = InetAddress.getByName(ip);
    } catch (UnknownHostException e) {
      LOG.warn("[Metron] No result found for IP {}", ip, e);
      return true;
    }
    if (isIneligibleAddress(ip, addr)) {
      LOG.debug("[Metron] IP ineligible for lookup {}", ip);
      return true;
    }
    return false;
  }

  default boolean isIneligibleAddress(String ipStr, InetAddress addr) {
    return addr.isAnyLocalAddress() || addr.isLoopbackAddress()
        || addr.isSiteLocalAddress() || addr.isMulticastAddress()
        || !ipvalidator.isValidInet4Address(ipStr);
  }
}
