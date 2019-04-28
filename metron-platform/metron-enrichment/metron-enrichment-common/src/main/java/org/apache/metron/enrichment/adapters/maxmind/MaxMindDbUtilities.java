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
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utilities class for working with MaxMind GeoLite2 databases. In particular, when the DB is stored on HDFS.
 */
public enum MaxMindDbUtilities {
  INSTANCE;

  static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static InetAddressValidator ipvalidator = new InetAddressValidator();

  /**
   * Determines if an IP is ineligible. In particular, this is used to filter before querying the database, as that requires a readlock to be set.
   * @param ip The IP to be tested
   * @return true if invalid, false otherwise
   */
  public static boolean invalidIp(String ip) {
    LOG.trace("Called validateIp({})", ip);
    InetAddress addr;
    try {
      addr = InetAddress.getByName(ip);
    } catch (UnknownHostException e) {
      LOG.warn("No result found for IP {}", ip, e);
      return true;
    }
    if (isIneligibleAddress(ip, addr)) {
      LOG.debug("IP ineligible for lookup {}", ip);
      return true;
    }
    return false;
  }

  /**
   * Determines if an address isn't eligible for getting appropriate results from the underlying database.
   * @param ipStr The IP String
   * @param addr The addr to be tested
   * @return True if ineligible, false otherwise
   */
  public static boolean isIneligibleAddress(String ipStr, InetAddress addr) {
    return addr.isAnyLocalAddress() || addr.isLoopbackAddress()
        || addr.isSiteLocalAddress() || addr.isMulticastAddress()
        || !ipvalidator.isValidInet4Address(ipStr);
  }

  /**
   * Logs and rethrows an IOException in a common way across implementations.
   * @param hdfsFile The hdfsFile we were trying to read from
   * @param e The exception we saw
   */
  public static void handleDatabaseIOException(String hdfsFile, IOException e) {
    LOG.error("Unable to open new database file {}", hdfsFile, e);
    throw new IllegalStateException("Unable to update MaxMind database");
  }

  /**
   * Reads a new Database from a given HDFS file
   * @param reader The DatabaseReader to use to read the file
   * @param hdfsFile The HDFS file to read
   * @param is An InputStream for use with the reader
   * @return The DatabaseReader that is set up with the new file
   * @throws IOException If there is an issue reading the file.
   */
  public static DatabaseReader readNewDatabase(DatabaseReader reader, String hdfsFile, InputStream is) throws IOException {
    LOG.info("Update to GeoIP data started with {}", hdfsFile);
    // InputStream based DatabaseReaders are always in memory.
    DatabaseReader newReader = new DatabaseReader.Builder(is).withCache(new CHMCache()).build();
    // If we've never set a reader, don't close the old one
    if (reader != null) {
      reader.close();
    }
    LOG.info("Finished update to GeoIP data started with {}", hdfsFile);
    return newReader;
  }

  /**
   * Retrieves the FileSystem
   * @return A FileSystem object used to retrieve the the database
   */
  public static FileSystem getFileSystem() {
    FileSystem fs;
    try {
      fs = FileSystem.get(new Configuration());
    } catch (IOException e) {
      LOG.error("Unable to retrieve get HDFS FileSystem");
      throw new IllegalStateException("Unable to get HDFS FileSystem");
    }
    return fs;
  }

  /**
   * Converts null to empty string
   * @param raw The raw object
   * @return Empty string if null, or the String value if not
   */
  public static String convertNullToEmptyString(Object raw) {
    return raw == null ? "" : String.valueOf(raw);
  }
}
