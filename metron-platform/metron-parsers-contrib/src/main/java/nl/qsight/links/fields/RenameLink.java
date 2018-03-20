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

import java.util.Map;

/**
 * A link which renames keys.
 */
public class RenameLink extends ChainLink {

    private Map<String, String> renames;

    /**
     * Get the renames.
     *
     * @return All the renames.
     */
    public Map<String, String> getRenames() {
        return renames;
    }

    /**
     * Set the renames.
     *
     * @param renames The key-value pairs where the keys are to original keys and the values are the field names after
     *                the renames.
     */
    public void setRenames(Map<String, String> renames) {
        this.renames = renames;
    }

    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config.containsKey("rename")) {
            assert config.get("rename") instanceof Map;
            this.setRenames((Map<String, String>) config.get("rename"));
        }
    }

    /**
     * Rename fields using the rename rules.
     *
     * @param data Input data.
     * @return Data with renamed keys.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JSONObject parse(JSONObject data) {
        if (this.getRenames() == null || this.getRenames().size() == 0)
            throw new IllegalStateException("No renames specified");

        JSONObject store = new JSONObject();
        for (String key : this.getRenames().keySet()) {
            if (data.containsKey(key)) {
                store.put(key, data.get(key));
            }
        }
        for (String key : this.getRenames().keySet()) {
            if (store.containsKey(key) && data.containsKey(key)) {
                String outputKey = getRenames().get(key);
                data.put(outputKey, store.get(key));
                data.remove(key);
            }
        }

        return data;
    }

}
