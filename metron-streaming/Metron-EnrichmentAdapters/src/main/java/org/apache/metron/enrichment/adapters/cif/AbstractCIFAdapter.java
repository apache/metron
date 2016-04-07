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

package org.apache.metron.enrichment.adapters.cif;

import java.io.Serializable;

import org.apache.metron.enrichment.bolt.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;

public abstract class AbstractCIFAdapter implements EnrichmentAdapter<CacheKey>,Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5040559164824221816L;
	protected static final Logger LOG = LoggerFactory
			.getLogger(AbstractCIFAdapter.class);
	
	abstract public boolean initializeAdapter();
	abstract public String enrichByIP(String metadata);
	abstract public String enrichByDomain(String metadata);
	abstract public String enrichByEmail(String metadata);

	@Override
	public void cleanup() {

	}
}
