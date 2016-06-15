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

package org.apache.metron.common.field.transformation;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.*;

public class TransformDateToUTCEpoch implements FieldTransformation {

    private final static HashSet<String> TIMEZONES = new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));
    private static final String TIMEZONE = "timeZone";

    private String getValueFromConfig(Map<String, Object> fieldMappingConfig, String key) {
        Object conditionObj = fieldMappingConfig.get(key);

        if(conditionObj == null || !(conditionObj instanceof String)) {
            return null;
        }
        return conditionObj.toString();
    }

    @Override
    public Map<String, Object> map( Map<String, Object> input
            , final List<String> outputFields
            , Map<String, Object> fieldMappingConfig
            , Map<String, Object> sensorConfig) {

        Map<String, Object> returnMap = new HashMap<>();
        String timeZone = getValueFromConfig(fieldMappingConfig, TIMEZONE);

        if (timeZone == null || !TIMEZONES.contains(timeZone))
            timeZone = "UTC";

        DateTime jodaDateTime = new DateTime(Long.valueOf(input.get("timestamp").toString()),
                DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone)));

        returnMap.put("timestamp", jodaDateTime.getMillis());
        return returnMap;
    }


}
