package org.apache.metron.enrichment.adapters.asn;/*
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
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AsnDatabase {
  INSTANCE;

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String ASN_HDFS_FILE = "asn.hdfs.file";
  public static final String ASN_HDFS_FILE_DEFAULT = "/apps/metron/asn/default/GeoLite2-ASN.tar.gz";

  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private static final Lock readLock = lock.readLock();
  private static final Lock writeLock = lock.writeLock();
  private static InetAddressValidator ipvalidator = new InetAddressValidator();
  private static volatile String hdfsLoc = ASN_HDFS_FILE_DEFAULT;
  private static DatabaseReader reader = null;

  public enum AsnProps {
    NETWORK("network"),
    ASN("autonomous_system_number"),
    ASO("autonomous_system_organization");
    Function<Map<String, Object>, Object> getter;
    String simpleName;

    AsnProps(String simpleName) {
      this(simpleName, m -> m.get(simpleName));
    }

    AsnProps(String simpleName,
        Function<Map<String, Object>, Object> getter
    ) {
      this.simpleName = simpleName;
      this.getter = getter;
    }

    public String getSimpleName() {
      return simpleName;
    }

    public Object get(Map<String, Object> map) {
      return getter.apply(map);
    }

    public void set(Map<String, Object> map, Object val) {
      map.put(simpleName, val);
    }
  }

  public synchronized void updateIfNecessary(Map<String, Object> globalConfig) {
    // Reload database if necessary (file changes on HDFS)
    LOG.trace("[Metron] Determining if GeoIpDatabase update required");
    String hdfsFile = ASN_HDFS_FILE_DEFAULT;
    if (globalConfig != null) {
      hdfsFile = (String) globalConfig.getOrDefault(ASN_HDFS_FILE, ASN_HDFS_FILE_DEFAULT);
    }

    // Always update if we don't have a DatabaseReader
    if (reader == null || !hdfsLoc.equals(hdfsFile)) {
      // Update
      hdfsLoc = hdfsFile;
      update(hdfsFile);
    } else {
      LOG.trace("[Metron] Update to GeoIpDatabase unnecessary");
    }
  }

  public void update(String hdfsFile) {
    // If nothing is set (or it's been unset, use the defaults)
    if (hdfsFile == null || hdfsFile.isEmpty()) {
      LOG.debug("[Metron] Using default for {}: {}", ASN_HDFS_FILE, ASN_HDFS_FILE_DEFAULT);
      hdfsFile = ASN_HDFS_FILE_DEFAULT;
    }

    FileSystem fs;
    try {
      fs = FileSystem.get(new Configuration());
    } catch (IOException e) {
      LOG.error("[Metron] Unable to retrieve get HDFS FileSystem");
      throw new IllegalStateException("[Metron] Unable to get HDFS FileSystem");
    }

    try (TarArchiveInputStream tis = new TarArchiveInputStream(
        new GZIPInputStream(fs.open(new Path(hdfsFile))))) {
      writeLock.lock();
      LOG.info("[Metron] Update to GeoIP data started with {}", hdfsFile);
      // InputStream based DatabaseReaders are always in memory.
      // Need to find the mmdb entry.
      TarArchiveEntry entry = tis.getNextTarEntry();
      while (entry != null) {
        if (entry.isFile() && entry.getName().endsWith(".mmdb")) {
          InputStream mmdb = new BufferedInputStream(tis); // Read directly from tarInput

          DatabaseReader newReader = new DatabaseReader.Builder(mmdb).withCache(new CHMCache())
              .build();
          DatabaseReader oldReader = reader;
          reader = newReader;
          // If we've never set a reader, don't close the old one
          if (oldReader != null) {
            oldReader.close();
          }
          LOG.info("[Metron] Finished update to GeoIP data started with {}", hdfsFile);
        }
        entry = tis.getNextTarEntry();
      }


    } catch (IOException e) {
      LOG.error("[Metron] Unable to open new database file {}", hdfsFile, e);
      throw new IllegalStateException("[Metron] Unable to update MaxMind database");
    } finally {
      // Don't unlock if the try failed
      if (lock.isWriteLocked()) {
        writeLock.unlock();
      }
    }
  }

  // Optional.empty means that we don't have any ASN information in database.
  // Optional exists, but empty means local IP (valid, but no info will be in the DB)
  public Optional<Map<String, Object>> get(String ip) {
    // Call get every single time, returns current version. Updates behind the scenes.
    LOG.trace("[Metron] Called AsnDatabase.get({})", ip);
    InetAddress addr;
    try {
      addr = InetAddress.getByName(ip);
    } catch (UnknownHostException e) {
      LOG.warn("[Metron] No result found for IP {}", ip, e);
      return Optional.empty();
    }
    if (isIneligibleAddress(ip, addr)) {
      LOG.debug("[Metron] IP ineligible for GeoLite2 lookup {}", ip);
      return Optional.empty();
    }

    try {
      readLock.lock();
      addr = InetAddress.getByName(ip);
      AsnResponse asnResponse = reader.asn(addr);
      HashMap<String, Object> asnInfo = new HashMap<>();
      AsnProps.ASN.set(asnInfo, asnResponse.getAutonomousSystemNumber());
      AsnProps.ASO
          .set(asnInfo, convertNullToEmptyString(asnResponse.getAutonomousSystemOrganization()));
      AsnProps.NETWORK.set(asnInfo, convertNullToEmptyString(asnResponse.getIpAddress()));

      return Optional.of(asnInfo);
    } catch (UnknownHostException | AddressNotFoundException e) {
      LOG.debug("[Metron] No result found for IP {}", ip);
    } catch (GeoIp2Exception | IOException e) {
      LOG.warn("[Metron] GeoLite2 DB encountered an error", e);
    } finally {
      readLock.unlock();
    }
    return Optional.empty();
  }

  protected String convertNullToEmptyString(Object raw) {
    return raw == null ? "" : String.valueOf(raw);
  }

  private boolean isIneligibleAddress(String ipStr, InetAddress addr) {
    return addr.isAnyLocalAddress() || addr.isLoopbackAddress()
        || addr.isSiteLocalAddress() || addr.isMulticastAddress()
        || !ipvalidator.isValidInet4Address(ipStr);
  }
}
