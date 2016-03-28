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

package org.apache.metron.dataloads.nonbulk.taxii;

import com.google.common.base.Joiner;
import org.apache.metron.dataloads.extractor.stix.types.ObjectTypeHandlers;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TaxiiConnectionConfig {
  final static ObjectMapper _mapper = new ObjectMapper();
  private URL endpoint;
  private int port = 443;
  private URL proxy;
  private String username;
  private String password;
  private ConnectionType type;
  private String collection = "default";
  private String subscriptionId = null;
  private Date beginTime;
  private String table;
  private String columnFamily;
  private Set<String> allowedIndicatorTypes = new HashSet<String>();

  public TaxiiConnectionConfig withAllowedIndicatorTypes(List<String> indicatorTypes) {
    allowedIndicatorTypes = new HashSet(indicatorTypes);
    return this;
  }

  public TaxiiConnectionConfig withTable(String table) {
    this.table = table;
    return this;
  }
  public TaxiiConnectionConfig withColumnFamily(String cf) {
    this.columnFamily = cf;
    return this;
  }
  public TaxiiConnectionConfig withBeginTime(Date time) {
    this.beginTime = time;
    return this;
  }
  public TaxiiConnectionConfig withSubscriptionId(String subId) {
    this.subscriptionId = subId;
    return this;
  }
  public TaxiiConnectionConfig withCollection(String collection) {
    this.collection = collection;
    return this;
  }

  public TaxiiConnectionConfig withPort(int port) {
    this.port = port;
    return this;
  }
  public TaxiiConnectionConfig withEndpoint(URL endpoint) {
    this.endpoint = endpoint;
    return this;
  }
  public TaxiiConnectionConfig withProxy(URL proxy) {
    this.proxy = proxy;
    return this;
  }
  public TaxiiConnectionConfig withUsername(String username) {
    this.username = username;
    return this;
  }
  public TaxiiConnectionConfig withPassword(String password) {
    this.password = password;
    return this;
  }
  public TaxiiConnectionConfig withConnectionType(ConnectionType type) {
    this.type= type;
    return this;
  }

  public void setEndpoint(String endpoint) throws MalformedURLException {
    this.endpoint = new URL(endpoint);
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setProxy(String proxy) throws MalformedURLException {
    this.proxy = new URL(proxy);
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setType(ConnectionType type) {
    this.type = type;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public void setBeginTime(String beginTime) throws ParseException {
    SimpleDateFormat sdf = (SimpleDateFormat)DateFormat.getDateInstance(DateFormat.MEDIUM);
    this.beginTime = sdf.parse(beginTime);
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getColumnFamily() {
    return columnFamily;
  }

  public void setColumnFamily(String columnFamily) {
    this.columnFamily = columnFamily;
  }

  public Date getBeginTime() {
    return beginTime;
  }
  public int getPort() {
    return port;
  }
  public URL getEndpoint() {
    return endpoint;
  }

  public URL getProxy() {
    return proxy;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public ConnectionType getType() {
    return type;
  }

  public String getCollection() {
    return collection;
  }
  public String getSubscriptionId() {
    return subscriptionId;
  }

  public void setAllowedIndicatorTypes(List<String> allowedIndicatorTypes) {
    withAllowedIndicatorTypes(allowedIndicatorTypes);
  }

  public Set<String> getAllowedIndicatorTypes() {
    return allowedIndicatorTypes;
  }
  public static synchronized TaxiiConnectionConfig load(InputStream is) throws IOException {
    TaxiiConnectionConfig ret = _mapper.readValue(is, TaxiiConnectionConfig.class);
    return ret;
  }
  public static synchronized TaxiiConnectionConfig load(String s, Charset c) throws IOException {
    return load( new ByteArrayInputStream(s.getBytes(c)));
  }
  public static synchronized TaxiiConnectionConfig load(String s) throws IOException {
    return load( s, Charset.defaultCharset());
  }

  @Override
  public String toString() {
    return "TaxiiConnectionConfig{" +
            "endpoint=" + endpoint +
            ", port=" + port +
            ", proxy=" + proxy +
            ", username='" + username + '\'' +
            ", password=" + (password == null?"null" : "'******'") +
            ", type=" + type +
            ", allowedIndicatorTypes=" + Joiner.on(',').join(allowedIndicatorTypes)+
            ", collection='" + collection + '\'' +
            ", subscriptionId='" + subscriptionId + '\'' +
            ", beginTime=" + beginTime +
            ", table=" + table + ":" + columnFamily+
            '}';
  }
}
