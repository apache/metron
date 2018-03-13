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
package org.apache.metron.performance.load.monitor.writers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.metron.performance.load.monitor.Results;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class ConsoleWriter implements Consumer<Writable> {

  private String getSummary(DescriptiveStatistics stats) {
    return String.format("Mean: %d, Std Dev: %d", (int)stats.getMean(), (int)Math.sqrt(stats.getVariance()));
  }

  @Override
  public void accept(Writable writable) {
    List<String> parts = new ArrayList<>();
    Date date = writable.getDate();
    for(Results r : writable.getResults()) {
      Long eps = r.getEps();
      if(eps != null) {
        String part = String.format(r.getFormat(), eps);
        if (r.getHistory().isPresent()) {
          part += " (" + getSummary(r.getHistory().get()) + ")";
        }
        parts.add(part);
      }
    }
    if(date != null) {
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      String header = dateFormat.format(date) + " - ";
      String emptyHeader = StringUtils.repeat(" ", header.length());
      for (int i = 0; i < parts.size(); ++i) {
        String part = parts.get(i);
        if (i == 0) {
          System.out.println(header + (part == null ? "" : part));
        } else {
          System.out.println(emptyHeader + (part == null ? "" : part));
        }
      }
    }
  }
}
