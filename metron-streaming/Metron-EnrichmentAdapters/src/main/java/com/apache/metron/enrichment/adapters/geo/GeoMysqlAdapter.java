/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.metron.enrichment.adapters.geo;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class GeoMysqlAdapter extends AbstractGeoAdapter {

	private Connection connection = null;
	private Statement statement = null;
	private String _ip;
	private String _username;
	private String _password;
	private String _tablename;
	private InetAddressValidator ipvalidator = new InetAddressValidator();

	public GeoMysqlAdapter(String ip, int port, String username,
			String password, String tablename) {
		try {
			_ip = InetAddress.getByName(ip).getHostAddress();

			boolean reachable = checkIfReachable(ip, 500);

			if (!reachable)
				throw new Exception("Unable to reach IP " + _ip
						+ " with username " + _username + " and password "
						+ _password + " accessing table name " + _tablename);

		} catch (Exception e) {
			_LOG.error("Environment misconfigured, cannot reach MYSQL server....");
			e.printStackTrace();
		}

		_username = username;
		_password = password;
		_tablename = tablename;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject enrich(String metadata) {

		ResultSet resultSet = null;

		try {

			_LOG.trace("[Metron] Received metadata: " + metadata);

			InetAddress addr = InetAddress.getByName(metadata);

			if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
					|| addr.isSiteLocalAddress() || addr.isMulticastAddress()
					|| !ipvalidator.isValidInet4Address(metadata)) {
				_LOG.trace("[Metron] Not a remote IP: " + metadata);
				_LOG.trace("[Metron] Returning enrichment: " + "{}");

				return new JSONObject();
			}

			_LOG.trace("[Metron] Is a valid remote IP: " + metadata);

			statement = connection.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			String locid_query = "select IPTOLOCID(\"" + metadata
					+ "\") as ANS";
			resultSet = statement.executeQuery(locid_query);

			if (resultSet == null)
				throw new Exception("Invalid result set for metadata: "
						+ metadata + ". Query run was: " + locid_query);

			resultSet.last();
			int size = resultSet.getRow();

			if (size == 0)
				throw new Exception("No result returned for: " + metadata
						+ ". Query run was: " + locid_query);

			resultSet.beforeFirst();
			resultSet.next();

			String locid = null;
			locid = resultSet.getString("ANS");

			if (locid == null)
				throw new Exception("Invalid location id for: " + metadata
						+ ". Query run was: " + locid_query);

			String geo_query = "select * from location where locID = " + locid
					+ ";";
			resultSet = statement.executeQuery(geo_query);

			if (resultSet == null)
				throw new Exception(
						"Invalid result set for metadata and locid: "
								+ metadata + ", " + locid + ". Query run was: "
								+ geo_query);

			resultSet.last();
			size = resultSet.getRow();

			if (size == 0)
				throw new Exception(
						"No result id returned for metadata and locid: "
								+ metadata + ", " + locid + ". Query run was: "
								+ geo_query);

			resultSet.beforeFirst();
			resultSet.next();

			JSONObject jo = new JSONObject();
			jo.put("locID", resultSet.getString("locID"));
			jo.put("country", resultSet.getString("country"));
			jo.put("city", resultSet.getString("city"));
			jo.put("postalCode", resultSet.getString("postalCode"));
			jo.put("latitude", resultSet.getString("latitude"));
			jo.put("longitude", resultSet.getString("longitude"));
			jo.put("dmaCode", resultSet.getString("dmaCode"));
			jo.put("locID", resultSet.getString("locID"));
			
			jo.put("location_point", jo.get("longitude") + "," + jo.get("latitude"));

			_LOG.debug("Returning enrichment: " + jo);

			return jo;

		} catch (Exception e) {
			e.printStackTrace();
			_LOG.error("Enrichment failure: " + e);
			return new JSONObject();
		}
	}

	@Override
	public boolean initializeAdapter() {

		_LOG.info("[Metron] Initializing MysqlAdapter....");

		try {

			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://" + _ip
					+ "/" + _tablename + "?user=" + _username + "&password="
					+ _password);

			connection.setReadOnly(true);

			if (!connection.isValid(0))
				throw new Exception("Invalid connection string....");

			_LOG.info("[Metron] Set JDBC connection....");

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			_LOG.error("[Metron] JDBC connection failed....");

			return false;
		}

	}
}
