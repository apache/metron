/*
 *
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
package org.apache.metron.common.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.function.Function;

public enum SerializationUtils implements Function<Object, byte[]> {
  INSTANCE;
  protected static final Logger LOG = LoggerFactory.getLogger(SerializationUtils.class);
  ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>() {

    @Override
    protected Kryo initialValue() {
      return new Kryo();
    }
  };

  /**
   * Applies this function to the given argument.
   *
   * @param t the function argument
   * @return the function result
   */
  @Override
  public byte[] apply(Object t) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Output output = new Output(bos);
      kryo.get().writeObject(output, t);
      output.flush();
      byte[] ret = bos.toByteArray();
      return ret;
    }
    catch(Throwable ex) {
      LOG.error("Unable to serialize " + t + ": " + ex.getMessage(), ex);
      throw ex;
    }
  }
}
