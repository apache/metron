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
package org.apache.metron.parsers.contrib.chainlink;

import org.apache.metron.parsers.contrib.common.Constants;
import org.json.simple.JSONObject;

/**
 * A special ChainLink that works with an input and one or more outputs. If the output is a JSONObject, then the output
 * is automatically merged with the current state. Otherwise, a special Constants.OUTPUT_MARKER field is created
 * containing the output. The type of the input should be specified when creating new classes. The field specified
 * by the Constants.INPUT_MARKER is casted to the desired input type.
 *
 * The RenderLink is capable of transforming multiple fields into a single input. Therefore, a combination of the
 * RenderLink and the ChainLinkIO is capable of transforming one or more inputs to one or more outputs and covers
 * all possible usages.
 *
 * @see ChainLink
 */
public abstract class ChainLinkIO<T> extends ChainLink {

    /**
     * This method parses the given input JSONObject and produces an updated JSONObject.
     *
     * @param input The JSONObject used as input.
     * @return The updated JSONObject.
     */
    public abstract Object parseInputField(T input);

    /**
     * This method parses the given input JSONObject and produces an updated JSONObject.
     *
     * @param data The JSONObject used as input.
     * @return The updated JSONObject.
     */
    @SuppressWarnings("unchecked")
    public JSONObject parse(JSONObject data) {
        String field = Constants.INPUT_MARKER;
        if (!data.containsKey(field)) field = Constants.ORIGINAL_STRING;
        if (!data.containsKey(field)) {
            throw new IllegalStateException("Field \"" + field + "\" not found in the state.");
        }
        T input = (T) data.get(field);
        Object outputObject = this.parseInputField(input);
        if (outputObject instanceof JSONObject) {
            JSONObject output = (JSONObject)outputObject;
            for (Object keyObject : output.keySet()) {
                String key = (String) keyObject;
                data.put(key, output.get(keyObject));
            }
        } else {
            data.put(Constants.OUTPUT_MARKER, outputObject);
        }

        // Clean up the input data
        data.remove(Constants.INPUT_MARKER);

        return data;
    }

}
