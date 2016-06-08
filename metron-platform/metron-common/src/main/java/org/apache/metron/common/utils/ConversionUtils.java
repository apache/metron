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

package org.apache.metron.common.utils;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.ConvertUtilsBean;

public class ConversionUtils {
  private static ThreadLocal<ConvertUtilsBean> UTILS_BEAN  = new ThreadLocal<ConvertUtilsBean>() {
    @Override
    protected ConvertUtilsBean initialValue() {
      ConvertUtilsBean ret = BeanUtilsBean2.getInstance().getConvertUtils();
      ret.deregister();
      ret.register(false,true, 1);
      return ret;
    }
  };
  public static <T> T convert(Object o, Class<T> clazz) {
    if(o == null) {
      return null;
    }
    return clazz.cast(UTILS_BEAN.get().convert(o, clazz));
  }
}
