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
package org.apache.metron.parsers.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldValidators {

    public static boolean isValidPort(int portNumber) {
        if (portNumber > 1 && portNumber < 65536)
            return true;
        else
            return false;
    }

    public static boolean isValidIpAddr(String ipAddress) {
        String pattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(ipAddress);
        if (m.matches())
            return true;
        else
            return false;
    }
}
