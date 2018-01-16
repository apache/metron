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

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.timing.StackWatch;

public class TimingCommand implements SpecialCommand {
  public static final String MAGIC_TIMING = "%timing";

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> startsWith(trimToEmpty(input), MAGIC_TIMING);
  }

  @Override
  public String getCommand() {
    return MAGIC_TIMING;
  }

  @Override
  public StellarResult execute(String command, StellarShellExecutor executor) {

    List<String> filter = new ArrayList<>();

    // if '%functions FOO' then show only functions that contain 'FOO'
    String startsWith = StringUtils.trimToEmpty(command.substring(MAGIC_TIMING.length()));

    if (StringUtils.isNotBlank(startsWith)) {
      filter.addAll(Arrays.asList(startsWith.trim().split("\\s+")));

    }

    Optional<StackWatch> lastWatch = executor.getLastWatch();
    String ret = StringUtils.EMPTY;
    if (lastWatch.isPresent()) {
      ret = formatWatchOutput(lastWatch.get(), filter);
    } else {
      ret = "No timing recorded";
    }
    return StellarResult.success(ret);
  }

  private String formatWatchOutput(StackWatch watch, final List<String> filterList) {
    final StringBuffer buff = new StringBuffer();
    watch.visit(((level, node) -> {
      final List<String> tags = node.getTags() == null ? new ArrayList<>(): Arrays.asList(node.getTags());
      if (filterList.size() > 0 && level != 0 && tags.size() > 0) {
        // if we have tags, but the node doesn't have them we should return
        boolean found = false;
        for (String tag : tags) {
          if(filterList.contains(tag)){
            found = true;
            break;
          }
        }
        if (!found) {
          return;
        }
      }
      for (int i = 0; i < level; i++) {
        buff.append("-");
      }
      buff.append("->");
      buff.append(node.getTimingName());
      if (node.getTags() != null && node.getTags().length > 0) {
        buff.append("[").append(String.join(",",node.getTags())).append("]");
      }
      buff.append(" : ").append(node.getStopWatch().getTime()).append("ms : ")
          .append(node.getStopWatch().getNanoTime()).append("ns").append("\n");
    }));
    return buff.toString();
  }
}
