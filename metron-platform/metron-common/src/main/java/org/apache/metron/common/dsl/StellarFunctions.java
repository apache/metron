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

package org.apache.metron.common.dsl;

import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.dsl.functions.resolver.SingletonFunctionResolver;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StellarFunctions {

  private static FunctionResolver resolver = SingletonFunctionResolver.getInstance();
  private static boolean initialized = false;
  private static ReadWriteLock lock = new ReentrantReadWriteLock();

  public static FunctionResolver FUNCTION_RESOLVER() {
    try {
      lock.readLock().lock();
      return resolver;
    }
    finally{
      lock.readLock().unlock();
    }
  }

  public static void setResolver(FunctionResolver r, Context context) {
    try {
      lock.writeLock().lock();
      resolver = r;
      if(initialized) {
        resolver.initialize(context);
      }
    }
    finally {
      lock.writeLock().unlock();
    }
  }


  public static void initialize(Context context) {
    try {
      lock.readLock().lock();
      initialized = true;
      resolver.initialize(context);
    }
    finally {
      lock.readLock().unlock();
    }
  }
}
