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

import java.io.Serializable;
import java.net.InetAddress;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apache.metron.enrichment.common.GenericEnrichmentBolt;
import com.apache.metron.enrichment.interfaces.EnrichmentAdapter;

@SuppressWarnings("serial")
public abstract class AbstractGeoAdapter implements EnrichmentAdapter,
		Serializable {

	protected static final Logger _LOG = LoggerFactory
			.getLogger(GenericEnrichmentBolt.class);

	abstract public JSONObject enrich(String metadata);

	abstract public boolean initializeAdapter();

	/**
	 * Check if we can reach the IP where geo data is storred
	 * 
	 * @param ip
	 *            - ip of geo database
	 * @param timeout
	 *            - timeout for a connection attempt
	 * @return - True if can connect, False if cannot
	 * @throws Exception
	 */
	public boolean checkIfReachable(String ip, int timeout) throws Exception {
		boolean reachable = InetAddress.getByName(ip).isReachable(timeout);

		if (!reachable)
			return false;

		return true;

	}

}
