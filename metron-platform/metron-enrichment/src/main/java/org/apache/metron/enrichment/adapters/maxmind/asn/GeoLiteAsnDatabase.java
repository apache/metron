package org.apache.metron.enrichment.adapters.maxmind.asn;/*
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
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import org.apache.metron.enrichment.adapters.maxmind.MaxMindDatabase;
import org.apache.metron.enrichment.adapters.maxmind.MaxMindDbUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages querying and updating of an Autonomous System Number (ASN) database provided by MaxMind.
 */
public enum GeoLiteAsnDatabase implements MaxMindDatabase {
  INSTANCE;

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String ASN_HDFS_FILE = "asn.hdfs.file";
  public static final String ASN_HDFS_FILE_DEFAULT = "/apps/metron/asn/default/GeoLite2-ASN.tar.gz";

  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private static final Lock readLock = lock.readLock();
  private static final Lock writeLock = lock.writeLock();
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

  @Override
  public String getHdfsFileConfig() {
    return ASN_HDFS_FILE;
  }

  @Override
  public String getHdfsFileDefault() {
    return ASN_HDFS_FILE_DEFAULT;
  }

  @Override
  public void lockIfNecessary() {
    writeLock.lock();
  }

  @Override
  public void unlockIfNecessary() {
    writeLock.unlock();
  }

  @Override
  public DatabaseReader getReader() {
    return reader;
  }

  @Override
  public void setReader(DatabaseReader reader) {
    GeoLiteAsnDatabase.reader = reader;
  }

  public synchronized void updateIfNecessary(Map<String, Object> globalConfig) {
    // Reload database if necessary (file changes on HDFS)
    LOG.trace("Determining if GeoLiteAsnDatabase update required");
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
      LOG.trace("Update to GeoLiteAsnDatabase unnecessary");
    }
  }

  /**
   * Retrieves the result fields based on the incoming IP address
   * @param ip The IP to lookup in the database
   * @return Optional.empty() if the IP address is invalid or not in the database.
   */
  public Optional<Map<String, Object>> get(String ip) {
    if (MaxMindDbUtilities.invalidIp(ip)) {
      return Optional.empty();
    }

    try {
      readLock.lock();
      InetAddress addr = InetAddress.getByName(ip);
      AsnResponse asnResponse = reader.asn(addr);
      HashMap<String, Object> asnInfo = new HashMap<>();
      AsnProps.ASN.set(asnInfo, asnResponse.getAutonomousSystemNumber());
      AsnProps.ASO
          .set(asnInfo, MaxMindDbUtilities.convertNullToEmptyString(asnResponse.getAutonomousSystemOrganization()));
      AsnProps.NETWORK
          .set(asnInfo, MaxMindDbUtilities.convertNullToEmptyString(asnResponse.getIpAddress()));

      return Optional.of(asnInfo);
    } catch (UnknownHostException | AddressNotFoundException e) {
      LOG.debug("No result found for IP {}", ip);
    } catch (GeoIp2Exception | IOException e) {
      LOG.warn("GeoLite2 ASN DB encountered an error", e);
    } finally {
      readLock.unlock();
    }
    return Optional.empty();
  }
}
