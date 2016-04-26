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
package org.apache.metron.enrichment.lookup;

import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;
import org.apache.metron.enrichment.lookup.handler.Handler;

import java.io.IOException;

public class Lookup<CONTEXT_T, KEY_T extends LookupKey, RESULT_T> implements Handler<CONTEXT_T, KEY_T, RESULT_T> {
  private String name;
  private AccessTracker accessTracker;
  private Handler<CONTEXT_T, KEY_T, RESULT_T> lookupHandler;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AccessTracker getAccessTracker() {
    return accessTracker;
  }

  public void setAccessTracker(AccessTracker accessTracker) {
    this.accessTracker = accessTracker;
  }

  public Handler< CONTEXT_T, KEY_T, RESULT_T > getLookupHandler() {
    return lookupHandler;
  }

  public void setLookupHandler(Handler< CONTEXT_T, KEY_T, RESULT_T > lookupHandler) {
    this.lookupHandler = lookupHandler;
  }

  @Override
  public boolean exists(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException {
    if(logAccess) {
      accessTracker.logAccess(key);
    }
    return lookupHandler.exists(key, context, logAccess);
  }

  @Override
  public RESULT_T get(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException {
    if(logAccess) {
      accessTracker.logAccess(key);
    }
    return lookupHandler.get(key, context, logAccess);
  }

  @Override
  public Iterable<Boolean> exists(Iterable<KEY_T> key, CONTEXT_T context, boolean logAccess) throws IOException {
    if(logAccess) {
      for (KEY_T k : key) {
        accessTracker.logAccess(k);
      }
    }
    return lookupHandler.exists(key, context, logAccess);
  }


  @Override
  public Iterable<RESULT_T> get(Iterable<KEY_T> key, CONTEXT_T context, boolean logAccess) throws IOException {
    if(logAccess) {
      for (KEY_T k : key) {
        accessTracker.logAccess(k);
      }
    }
    return lookupHandler.get(key, context, logAccess);
  }

  @Override
  public void close() throws Exception {
    accessTracker.cleanup();
    lookupHandler.close();
  }
}
