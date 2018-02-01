/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.common.shell.specials;

import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;

import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.metron.stellar.common.shell.StellarResult.noop;

/**
 * A special command that handles comments.
 *
 *    # this is a comment
 */
public class Comment implements SpecialCommand {

  public static final String COMMENT_PREFIX = "#";

  @Override
  public String getCommand() {
    return "#";
  }

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> startsWith(trimToEmpty(input), COMMENT_PREFIX);
  }

  @Override
  public StellarResult execute(String expression, StellarShellExecutor executor) {
    return noop();
  }
}
