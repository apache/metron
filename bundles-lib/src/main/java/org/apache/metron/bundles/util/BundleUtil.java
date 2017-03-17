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
package org.apache.metron.bundles.util;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.metron.bundles.BundleManifestEntry;
import org.apache.metron.bundles.bundle.BundleCoordinate;
import org.apache.metron.bundles.bundle.BundleDetails;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class BundleUtil {

    /**
     * Creates a BundleDetails from the given Bundle working directory.
     *
     * @param bundleDirectory the directory of an exploded Bundle which contains a META-INF/MANIFEST.MF
     *
     * @return the BundleDetails constructed from the information in META-INF/MANIFEST.MF
     */
    public static BundleDetails fromBundleDirectory(final FileObject bundleDirectory, BundleProperties props) throws FileSystemException, IllegalStateException {
        if (bundleDirectory == null) {
            throw new IllegalArgumentException("Bundle Directory cannot be null");
        }

        final FileObject manifestFile = bundleDirectory.resolveFile("META-INF/MANIFEST.MF");
        try (final InputStream fis = manifestFile.getContent().getInputStream()) {
            final Manifest manifest = new Manifest(fis);

            final Attributes attributes = manifest.getMainAttributes();
            final String prefix = props.getMetaIdPrefix();
            final BundleDetails.Builder builder = new BundleDetails.Builder();
            builder.workingDir(bundleDirectory);

            final String group = attributes.getValue(prefix + BundleManifestEntry.PRE_GROUP.getManifestName());
            final String id = attributes.getValue(prefix + BundleManifestEntry.PRE_ID.getManifestName());
            final String version = attributes.getValue(prefix + BundleManifestEntry.PRE_VERSION.getManifestName());
            builder.coordinate(new BundleCoordinate(group, id, version));

            final String dependencyGroup = attributes.getValue(prefix + BundleManifestEntry.PRE_DEPENDENCY_GROUP.getManifestName());
            final String dependencyId = attributes.getValue(prefix + BundleManifestEntry.PRE_DEPENDENCY_ID.getManifestName());
            final String dependencyVersion = attributes.getValue(prefix + BundleManifestEntry.PRE_DEPENDENCY_VERSION.getManifestName());
            if (!StringUtils.isBlank(dependencyId)) {
                builder.dependencyCoordinate(new BundleCoordinate(dependencyGroup, dependencyId, dependencyVersion));
            }

            builder.buildBranch(attributes.getValue(BundleManifestEntry.BUILD_BRANCH.getManifestName()));
            builder.buildTag(attributes.getValue(BundleManifestEntry.BUILD_TAG.getManifestName()));
            builder.buildRevision(attributes.getValue(BundleManifestEntry.BUILD_REVISION.getManifestName()));
            builder.buildTimestamp(attributes.getValue(BundleManifestEntry.BUILD_TIMESTAMP.getManifestName()));
            builder.buildJdk(attributes.getValue(BundleManifestEntry.BUILD_JDK.getManifestName()));
            builder.builtBy(attributes.getValue(BundleManifestEntry.BUILT_BY.getManifestName()));

            return builder.build();
        }catch(IOException ioe){
            throw new FileSystemException("failed reading manifest file " + manifestFile.getURL(),ioe);
        }
    }

}
