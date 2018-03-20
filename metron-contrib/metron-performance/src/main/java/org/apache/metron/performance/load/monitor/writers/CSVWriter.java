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

import com.google.common.base.Joiner;
import org.apache.metron.performance.load.monitor.Results;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CSVWriter implements Consumer<Writable> {
  private Optional<PrintWriter> pw = Optional.empty();

  public CSVWriter(File outFile) throws IOException {
    if(outFile != null) {
      pw = Optional.of(new PrintWriter(new FileWriter(outFile)));
    }
  }

  @Override
  public void accept(Writable writable) {
    if(pw.isPresent()) {
      List<String> parts = new ArrayList<>();
      parts.add("" + writable.getDate().getTime());
      for (Results r : writable.getResults()) {
        parts.add(r.getName());
        parts.add(r.getEps() == null?"":(r.getEps() + ""));
        if (r.getHistory().isPresent()) {
          parts.add("" + (int) r.getHistory().get().getMean());
          parts.add("" + (int) Math.sqrt(r.getHistory().get().getVariance()));
        } else {
          parts.add("");
          parts.add("");
        }
      }
      pw.get().println(Joiner.on(",").join(parts));
      pw.get().flush();
    }
  }

  public void close() {
    if(pw.isPresent()) {
      pw.get().close();
    }
  }
}
