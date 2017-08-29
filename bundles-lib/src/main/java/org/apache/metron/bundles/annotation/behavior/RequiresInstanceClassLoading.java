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
package org.apache.metron.bundles.annotation.behavior;

import java.lang.annotation.*;

/**
 *  Marker annotation a component can use to indicate that the framework should create a new ClassLoader
 *  for each instance of the component, copying all resources from the component's BundleClassLoader to a
 *  new ClassLoader which will only be used by a given instance of the component.
 *
 *  This annotation is typically used when a component has one or more PropertyDescriptors which set
 *  dynamicallyModifiesClasspath(boolean) to true.
 *
 *  When this annotation is used it is important to note that each added instance of the component will increase
 *  the overall memory footprint more than that of a component without this annotation.
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequiresInstanceClassLoading {
}
