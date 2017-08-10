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
package org.apache.metron.indexing.dao;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class IndexDaoFactory {
  public static List<IndexDao> create( String daoImpls
                                     , AccessConfig config
                                     ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
  {
    List<IndexDao> ret = new ArrayList<>();
    for(String daoImpl : Splitter.on(",").split(daoImpls)) {
      Class<? extends IndexDao> clazz = (Class<? extends IndexDao>) Class.forName(daoImpl);
      IndexDao instance = clazz.getConstructor().newInstance();
      instance.init(config);
      ret.add(instance);
    }
    return ret;
  }

  public static IndexDao combine(Iterable<IndexDao> daos) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    return combine(daos, x -> x);
  }

  public static IndexDao combine(Iterable<IndexDao> daos, Function<IndexDao, IndexDao> daoTransformation) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    int numDaos =  Iterables.size(daos);
    if(numDaos == 0) {
      throw new IllegalArgumentException("Trying to combine 0 dao's into a DAO is not a supported configuration.");
    }
    if( numDaos == 1) {
      return daoTransformation.apply(Iterables.getFirst(daos, null));
    }
    return new MultiIndexDao(daos, daoTransformation);
  }
}
