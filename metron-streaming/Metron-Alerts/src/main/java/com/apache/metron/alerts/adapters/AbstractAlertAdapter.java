/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensoc.alerts.adapters;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.opensoc.alerts.interfaces.AlertsAdapter;

@SuppressWarnings("serial")
public abstract class AbstractAlertAdapter implements AlertsAdapter, Serializable{
	
	protected static final Logger _LOG = LoggerFactory
			.getLogger(AbstractAlertAdapter.class);


	protected Cache<String, String> cache;
	
	protected String generateAlertId(String source_ip, String dst_ip,
			int alert_type) {

		String key = makeKey(source_ip, dst_ip, alert_type);

		if (cache.getIfPresent(key) != null)
			return cache.getIfPresent(key);

		String new_UUID = System.currentTimeMillis() + "-" + UUID.randomUUID();

		cache.put(key, new_UUID);
		key = makeKey(dst_ip, source_ip, alert_type);
		cache.put(key, new_UUID);

		return new_UUID;

	}
	
	private String makeKey(String ip1, String ip2, int alert_type) {
		return (ip1 + "-" + ip2 + "-" + alert_type);
	}
	
	protected void generateCache(int _MAX_CACHE_SIZE_OBJECTS_NUM, int _MAX_TIME_RETAIN_MINUTES)
	{
		cache = CacheBuilder.newBuilder().maximumSize(_MAX_CACHE_SIZE_OBJECTS_NUM)
				.expireAfterWrite(_MAX_TIME_RETAIN_MINUTES, TimeUnit.MINUTES).build();
	}
}
