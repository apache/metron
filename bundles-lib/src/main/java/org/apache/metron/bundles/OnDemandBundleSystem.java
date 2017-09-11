/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.bundles;

import java.net.URISyntaxException;
import java.util.Set;
import org.apache.commons.vfs2.FileSystemException;

public class OnDemandBundleSystem implements BundleSystem {
 private BundleSystemBuilder builder;
 private volatile BundleSystem bundleSystem;

 protected OnDemandBundleSystem(BundleSystemBuilder builder) {
  this.builder = new BundleSystemBuilder().withOtherBuilder(builder).withBundleSystemType(BundleSystemType.DEFAULT);
 }

 @Override
 public <T> T createInstance(String specificClassName, Class<T> clazz)
     throws ClassNotFoundException, InstantiationException, NotInitializedException, IllegalAccessException {
  return getBundleSystem().createInstance(specificClassName,clazz);
 }

 @Override
 public <T> Set<Class<? extends T>> getExtensionsClassesForExtensionType(Class<T> extensionType)
     throws NotInitializedException {
  return getBundleSystem().getExtensionsClassesForExtensionType(extensionType);
 }

 @Override
 public void addBundle(String bundleFileName)
     throws NotInitializedException, ClassNotFoundException, FileSystemException, URISyntaxException {
    getBundleSystem().addBundle(bundleFileName);
 }

 private BundleSystem getBundleSystem() throws NotInitializedException {
  BundleSystem bs = bundleSystem;
  if (bs == null) {
   synchronized (OnDemandBundleSystem.class) {
    bs = builder.build();
    bundleSystem = bs;
   }
  }
  return bs;
 }
}
