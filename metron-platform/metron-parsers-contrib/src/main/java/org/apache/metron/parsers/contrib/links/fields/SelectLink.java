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
import org.apache.metron.parsers.contrib.common.Constants;
import org.json.simple.JSONObject;

import java.util.Map;

/**
 * A link which selects a field an puts it into the input marker field.
 */
public class SelectLink extends ChainLink {

    private String field;

    /**
     * Get the field to select.
     *
     * @return The field.
     */
    public String getField() {
        return field;
    }

    /**
     * Set the field to select.
     *
     * @param field The field to select.
     */
    public void setField(String field) {
        this.field = field;
    }

    public void configure(Map<String, Object> config) {
        if (config.containsKey("field")) {
            assert config.get("field") instanceof String;
            this.setField((String) config.get("field"));
        }
    }

    /**
     * Copy the selected field to select to the input marker field.
     *
     * @param data Input data.
     * @return Data with an additional input marker field.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JSONObject parse(JSONObject data) {
        if (this.getField() == null) throw new IllegalStateException("The field to select should be specified.");

        data.put(Constants.INPUT_MARKER, data.get(this.getField()));

        return data;
    }

}
