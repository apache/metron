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
package org.apache.metron.parsers.contrib.chainparser;

import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.apache.metron.parsers.contrib.common.Constants;
import org.apache.metron.parsers.contrib.links.fields.IdentityLink;
import org.apache.metron.parsers.contrib.utils.ConfigUtils;
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.contrib.links.fields.IdentityLink;
import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.metron.parsers.contrib.common.Constants.ORIGINAL_STRING;
import static org.apache.metron.parsers.contrib.common.Constants.TIMESTAMP;

/**
 * The ChainParser is a composable unit consisting of a DAG of ChainLink objects. It points to the first ChainLink of
 * the ChainLink DAG and each ChainLink points towards the next ChainLink item in the DAG.
 */
public class ChainParser extends BasicParser {

    // The first ChainLink of the ChainLink DAG
    private ChainLink link;
    // The encoding of the messages
    private String encoding;

    // Pre link hook
    private Method preLinkHook = null;
    private Object preLinkInvoker = null;

    // Post link hook
    private Method postLinkHook = null;
    private Object postLinkInvoker = null;

    /**
     * Initializing the ChainParser.
     */
    public ChainParser() {
        this.link = new IdentityLink();
        this.setEncoding("UTF-8");
    }

    /**
     * An empty initialization function which is required by the BasicParser class.
     */
    @Override
    public void init() {

    }

    /**
     * Set the initial ChainLink of the ChainLink DAG.
     *
     * @param link The initial ChainLink.
     */
    public void setInitialLink(ChainLink link) {
        this.link = link;
    }

    /**
     * Get the initial ChainLink of the ChainLink DAG. The default ChainLink DAG consists of one IdentityLink which
     * copies the input to the output.
     *
     * @return The initial ChainLink.
     */
    public ChainLink getInitialLink() {
        return this.link;
    }

    /**
     * Configuration for the ChainParser.
     *
     * @param map A mapping from configuration items to configuration values.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> map) {
        map = ConfigUtils.compile(map);
        ChainLink linkObject = ConfigUtils.getRootLink(map);
        this.setInitialLink(linkObject);
    }

    public void setPreLinkHook(Method hook, Object invoker) {
        this.preLinkHook = hook;
        this.preLinkInvoker = invoker;
    }

    public void setPostLinkHook(Method hook, Object invoker) {
        this.postLinkHook = hook;
        this.postLinkInvoker = invoker;
    }

    private void executePreLinkHook(Object... args) {
        if (this.preLinkHook == null || this.preLinkInvoker == null) return;
        try {
            this.preLinkHook.invoke(this.preLinkInvoker, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("The post link is not executable.");
        }
    }

    private void executePostLinkHook(Object... args) {
        if (this.postLinkHook == null || this.postLinkInvoker == null) return;
        try {
            this.postLinkHook.invoke(this.postLinkInvoker, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("The post link is not executable.");
        }
    }

    /**
     * Parse a given string represented as a list of bytes into a list containing a single JSON object.
     *
     * @param bytes The byte representation of the input.
     * @return A list containing a single JSON object which is the parsed representation of the input.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<JSONObject> parse(byte[] bytes) {
        // Try to decode the bytes
        String decodedMessage;
        try {
            decodedMessage = new String(bytes, this.getEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding.");
        }

        // Put the bytes conversion and current date into an initial JSON object
        JSONObject state = new JSONObject();
        Instant instant = Instant.now();
        long timestamp = instant.toEpochMilli();
        state.put(ORIGINAL_STRING, decodedMessage);
        state.put(TIMESTAMP, timestamp);

        // Execute the parser DAG (Directed Acyclic Graph)
        ChainLink link = this.link;

        this.executePreLinkHook(link);
        state = this.executeLink(link, state);
        this.executePostLinkHook(link);

        // Iterate through the DAG until the end has reached
        while (link.hasNextLink()) {
            link = link.getNextLink();
            this.executePreLinkHook(link);
            state = this.executeLink(link, state);
            this.executePostLinkHook(link);
        }

        // Clean up the output marker
        if (state.containsKey(Constants.OUTPUT_MARKER)) {
            state.remove(Constants.OUTPUT_MARKER);
        }

        // Create a list containing a single item
        List<JSONObject> resultSet = new ArrayList<>();
        resultSet.add(state);

        // Return the result
        return resultSet;
    }

    /**
     * Execute a ChainLink and return the result.
     *
     * @param link  ChainLink to execute.
     * @param state Input data.
     * @return The output of the ChainLink.
     */
    private JSONObject executeLink(ChainLink link, JSONObject state) {
        String linkName = link.getClass().getCanonicalName();

        // Execute the link
        state = link.parse(state);

        // Check the validness of the state after executing the link
        if (!state.containsKey(ORIGINAL_STRING))
            throw new IllegalStateException("The state does not contain the \"original_string\" field after executing "
                    + linkName + ".");
        if (!state.containsKey(TIMESTAMP))
            throw new IllegalStateException("The state does not contain the \"timestamp\" field after executing "
                    + linkName + ".");

        // Clean up the input field when it still exists
        if (state.containsKey(Constants.INPUT_MARKER)) {
            state.remove(Constants.INPUT_MARKER);
        }

        return state;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

}
