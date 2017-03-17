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

/**
 * Enumeration of entries that will be in a BUNDLE MANIFEST file.
 */
public enum BundleManifestEntry {

    PRE_GROUP("-Group"),
    PRE_ID("-Id"),
    PRE_VERSION("-Version"),
    PRE_DEPENDENCY_GROUP("-Dependency-Group"),
    PRE_DEPENDENCY_ID("-Dependency-Id"),
    PRE_DEPENDENCY_VERSION("-Dependency-Version"),
    BUILD_TAG("Build-Tag"),
    BUILD_REVISION("Build-Revision"),
    BUILD_BRANCH("Build-Branch"),
    BUILD_TIMESTAMP("Build-Timestamp"),
    BUILD_JDK("Build-Jdk"),
    BUILT_BY("Built-By")
    ;

    final String manifestName;

    BundleManifestEntry(String manifestName) {
        this.manifestName = manifestName;
    }

    public String getManifestName() {
        return manifestName;
    }

}
