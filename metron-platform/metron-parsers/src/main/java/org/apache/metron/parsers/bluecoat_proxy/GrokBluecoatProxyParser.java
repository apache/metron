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

package org.apache.metron.parsers.bluecoat_proxy;

import org.apache.metron.parsers.GrokParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Iterator;

public class GrokBluecoatProxyParser extends GrokParser {

    private static final long serialVersionUID = 1179184051981797924L;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(GrokBluecoatProxyParser.class);

    public GrokBluecoatProxyParser(String grokHdfsPath, String patternLabel) {
        super(grokHdfsPath, patternLabel);
    }

    @Override
    protected void postParse(JSONObject message) {
        LOGGER.debug("Entered with postParse: " + message.toJSONString());
        removeEmptyFields(message);
    }

    @SuppressWarnings("unchecked")
    private void removeEmptyFields(JSONObject json) {
        LOGGER.debug("removing unnecessary fields");
        Iterator<Object> keyIter = json.keySet().iterator();
        while (keyIter.hasNext()) {
            Object key = keyIter.next();
            Object value = json.get(key);
            if (null == value || "".equals(value.toString()) || "-".equals(value.toString())) {
                keyIter.remove();
            }
        }
    }

    @Override
    protected long formatTimestamp(Object value) {
        LOGGER.debug("Formatting timestamp");
        long epochTimestamp = System.currentTimeMillis();
        if (value != null) {
            try {
                epochTimestamp = toEpoch(value.toString());
            } catch (ParseException e) {
                //default to current time
                LOGGER.debug("Unable to format time correctly. Using current system time");
            }
        }
        return epochTimestamp;
    }

}
