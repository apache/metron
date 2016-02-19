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
package org.apache.metron.alerts.adapters;

import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;

public class RangeChecker {

	static boolean checkRange(Set<String> CIDR_networks, String ip) {
		for (String network : CIDR_networks) {
				
			System.out.println("Looking at range: " + network + " and ip " + ip);
			SubnetUtils utils = new SubnetUtils(network);
			if(utils.getInfo().isInRange(ip)) {
				System.out.println(ip + " in range " + network);
				return true;
			}
		}
		
		//no matches
		return false;
	}
}
