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

package org.apache.metron.writer.hdfs;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback intended to be able to manage open files in {@link HdfsWriter}. This callback will close
 * the associated {@link SourceHandler} and remove it from the map of open files.
 */
public class SourceHandlerCallback {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  Map<SourceHandlerKey, SourceHandler> sourceHandlerMap;
  SourceHandlerKey key;
  SourceHandlerCallback(Map<SourceHandlerKey, SourceHandler> sourceHandlerMap, SourceHandlerKey key) {
    this.sourceHandlerMap = sourceHandlerMap;
    this.key = key;
  }

  /**
   * Removes {@link SourceHandler} from the map of open files. Also closes it to ensure resources such as
   * {@link java.util.Timer} is closed.
   */
  public void removeKey() {
    SourceHandler removed = sourceHandlerMap.remove(key);
    if(removed != null) {
      removed.close();
    }
    LOG.debug("Removed {} -> {}. Current state of sourceHandlerMap: {}",
        key,
        removed,
        sourceHandlerMap
    );
  }
}

