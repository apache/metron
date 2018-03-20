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
package nl.qsight.links.fields;

import nl.qsight.chainlink.ChainLink;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * A link which blacklists keys.
 */
public class BlacklistLink extends ChainLink {

    private List<String> fields;

    public void configure(Map<String, Object> config) {
        this.fields = null;
        if (config.containsKey("fields")) {
            assert config.get("fields") instanceof List;
            this.setFields((List<String>) config.get("fields"));
        }
    }

    /**
     * Get the fields.
     *
     * @return The fields.
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Set the fields.
     *
     * @param fields The fields which will be blacklisted.
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    /**
     * Whitelist all the specified fields and filter out fields not in the list.
     *
     * @param data Input data.
     * @return Data with only whitelisted fields.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JSONObject parse(JSONObject data) {
        if (this.getFields() == null) throw new IllegalStateException("The blacklisted fields should be specified.");
        JSONObject result = new JSONObject();

        for (Object keyObject : data.keySet()) {
            if (keyObject instanceof String) {
                String key = (String) keyObject;
                if (!this.getFields().contains(key)) {
                    result.put(keyObject, data.get(keyObject));
                }
            }
        }

        return result;
    }

}
