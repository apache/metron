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

import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.RenderResult;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.time.Year;
import java.util.List;
import java.util.Map;

/**
 * A link for rendering a template to a new field.
 */
public class RenderLink extends ChainLink {
    // The fields and constants to concatenate
    private String template;

    // The name of the field to store the output in
    private String outputField;

    // The used variables
    private List variables;

    /**
     * Get the template.
     *
     * @return The template.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Set the template.
     *
     * @param template The Jinja template.
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Get the output field.
     *
     * @return The field in which the output is stored.
     */
    public String getOutputField() {
        return outputField;
    }

    /**
     * Set the output field.
     *
     * @param field The field in which the output is stored.
     */
    public void setOutputField(String field) {
        this.outputField = field;
    }

    public void setVariables(List variables) { this.variables = variables; }

    public void configure(Map<String, Object> config) {
        this.variables = null;
        if (config.containsKey("template")) {
            assert config.get("template") instanceof String;
            this.setTemplate((String) config.get("template"));
        }
        if (config.containsKey("variables")) {
            assert config.get("variables") instanceof List;
            this.setVariables((List) config.get("variables"));
        }
        if (config.containsKey("output")) {
            assert config.get("output") instanceof String;
            this.setOutputField((String) config.get("output"));
        }
    }

    /**
     * Parse the JSON object. All keys of the input become available in the template. For example, if the input
     * contains {"user": "me"}, then the template "{{user}}" is rendered as "me".
     *
     * @param data The input data.
     * @return The output data in which the output field (specified by the setOutputField method) is filled with the
     * rendered version of the template (specified by the setTemplate method).
     */
    @Override
    @SuppressWarnings("unchecked")
    public JSONObject parse(JSONObject data) {
        if (this.getTemplate() == null) throw new IllegalStateException("No template specified.");
        if (this.getOutputField() == null) throw new IllegalStateException("No output field specified.");

        // Loop in O(n) time through the data and replace variables when found
        String template = this.getTemplate();

        boolean inVariable = false;
        String variableName = "";
        String buffer = "";
        int bracketOpenCount = 0;
        int bracketCloseCount = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);

            if (c == '{') {
                bracketOpenCount += 1;
            } else {
                bracketOpenCount = 0;
            }

            if (c == '}') {
                if (inVariable) {
                    inVariable = false;
                    variableName = variableName.trim();
                    buffer = buffer.substring(0, buffer.length() - 2);

                    // Substitute the variable
                    if (variableName.equals("year")) {
                        buffer += Year.now().toString();
                    } else {
                        if (data.containsKey(variableName)) {
                            buffer += data.get(variableName);
                        }
                    }

                    // Clear the variable name
                    variableName = "";
                }
                bracketCloseCount += 1;
            } else {
                bracketCloseCount = 0;
            }

            if (inVariable) {
                variableName += c;
            } else {
                buffer += c;
            }

            if (bracketOpenCount == 2) {
                inVariable = true;
            }

            if (bracketCloseCount == 2) {
                buffer = buffer.substring(0, buffer.length() - 2);
            }
        }

        data.put(this.getOutputField(), buffer);

        // Return the result
        return data;
    }
}
