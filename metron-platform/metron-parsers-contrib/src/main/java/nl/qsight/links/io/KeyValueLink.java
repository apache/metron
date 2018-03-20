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
package nl.qsight.links.io;

import nl.qsight.chainlink.ChainLinkIO;
import nl.qsight.utils.StringUtils;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * A link for splitting key-value pairs in strings.
 */
public class KeyValueLink extends ChainLinkIO<String> {

    private String keyValueDelimiter;
    private String pairDelimiter;
    private String validKeyChars;

    public String getKeyValueDelimiter() {
        return keyValueDelimiter;
    }

    public void setKeyValueDelimiter(String keyValueDelimiter) {
        this.keyValueDelimiter = keyValueDelimiter;
    }

    public String getPairDelimiter() {
        return pairDelimiter;
    }

    public void setPairDelimiter(String pairDelimiter) {
        this.pairDelimiter = pairDelimiter;
    }

    public String getValidKeyChars() {
        return validKeyChars;
    }

    public void setValidKeyChars(String validKeyChars) {
        this.validKeyChars = validKeyChars;
    }

    public void configure(Map<String, Object> config) {
        if (config.containsKey("pair_delimiter")) {
            assert config.get("pair_delimiter") instanceof String;
            this.setPairDelimiter((String) config.get("pair_delimiter"));
        }
        if (config.containsKey("key_value_delimiter")) {
            assert config.get("key_value_delimiter") instanceof String;
            this.setKeyValueDelimiter((String) config.get("key_value_delimiter"));
        }
        if (config.containsKey("valid_key_characters")) {
            assert config.get("valid_key_characters") instanceof String;
            this.setValidKeyChars((String) config.get("valid_key_characters"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object parseInputField(String input) {
        if (this.getKeyValueDelimiter() == null) throw new IllegalStateException("The key-value delimiter is not set.");
        if (this.getPairDelimiter() == null) throw new IllegalStateException("The pair delimiter is not set.");
        if (this.getValidKeyChars() == null) throw new IllegalStateException("The valid key characters are not set.");

        JSONObject result = new JSONObject();
        Map<String, String> pairs = StringUtils.parseKeyValuePairs(input, this.getKeyValueDelimiter(),
                this.getPairDelimiter(), this.getValidKeyChars());
        for (String key : pairs.keySet()) {
            if (key != null && !key.equals("null")) {
                result.put(key, pairs.get(key));
            }
        }
        return result;
    }
}
