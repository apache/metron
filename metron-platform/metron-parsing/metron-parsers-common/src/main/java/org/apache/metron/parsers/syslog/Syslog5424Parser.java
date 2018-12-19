/*
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

package org.apache.metron.parsers.syslog;

import com.github.palindromicity.syslog.AllowableDeviations;
import com.github.palindromicity.syslog.NilPolicy;
import com.github.palindromicity.syslog.SyslogParser;
import com.github.palindromicity.syslog.SyslogParserBuilder;
import com.github.palindromicity.syslog.SyslogSpecification;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;


/**
 * Parser for well structured RFC 5424 messages.
 */
public class Syslog5424Parser extends BaseSyslogParser implements Serializable {
  public static final String NIL_POLICY_CONFIG = "nilPolicy";

  @Override
  public SyslogParser buildSyslogParser(Map<String, Object> config) {
    // Default to OMIT policy for nil fields
    // this means they will not be in the returned field set
    String nilPolicyStr = (String) config.getOrDefault(NIL_POLICY_CONFIG, NilPolicy.OMIT.name());
    NilPolicy nilPolicy = NilPolicy.valueOf(nilPolicyStr);
    return new SyslogParserBuilder()
            .forSpecification(SyslogSpecification.RFC_5424)
            .withNilPolicy(nilPolicy)
            .withDeviations(EnumSet.of(AllowableDeviations.PRIORITY, AllowableDeviations.VERSION))
            .build();
  }
}

