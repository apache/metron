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
package org.apache.metron.parsers.contrib.links.fields;

import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.apache.metron.parsers.contrib.utils.StringUtils;
import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.apache.metron.parsers.contrib.utils.StringUtils;
import org.json.simple.JSONObject;

/**
 * A link for normalizing field names.
 */
public class NormalizeFieldLink extends ChainLink {

    /**
     *
     *
     * @param data Input data.
     * @return Data with only whitelisted fields.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JSONObject parse(JSONObject data) {
        JSONObject result = new JSONObject();

        for (Object keyObject : data.keySet()) {
            if (keyObject instanceof String) {
                String key = (String) keyObject;
                String newKey = StringUtils.normalize(key);
                // Remove trailing and beginning underscore
                if (newKey == null || newKey.length() == 0) {
                    newKey = null;
                }
                if (result.containsKey(newKey)) throw new IllegalStateException("Duplicate normalized keys.");
                result.put(newKey, data.get(keyObject));
            }
        }

        return result;
    }

}
