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
package org.apache.metron.bundles.bundle;

import org.apache.commons.vfs2.FileObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.vfs2.FileSystemException;

/**
 * Metadata about a bundle. the coordinates and bundleFile properties are required
 */
public class BundleDetails {

  private final FileObject bundleFile;

  private final BundleCoordinates coordinates;
  private final BundleCoordinates dependencyCoordinates;

  private final String buildTag;
  private final String buildRevision;
  private final String buildBranch;
  private final String buildTimestamp;
  private final String buildJdk;
  private final String builtBy;

  private BundleDetails(final Builder builder) {
    this.bundleFile = builder.bundleFile;
    this.coordinates = builder.coordinates;
    this.dependencyCoordinates = builder.dependencyCoordinates;

    this.buildTag = builder.buildTag;
    this.buildRevision = builder.buildRevision;
    this.buildBranch = builder.buildBranch;
    this.buildTimestamp = builder.buildTimestamp;
    this.buildJdk = builder.buildJdk;
    this.builtBy = builder.builtBy;

    if (this.coordinates == null) {
      if (this.bundleFile == null) {
        throw new IllegalStateException("Coordinate cannot be null");
      } else {
        throw new IllegalStateException(
            "Coordinate cannot be null for " + this.bundleFile.getName());
      }
    }

    if (this.bundleFile == null) {
      throw new IllegalStateException("bundleFile cannot be null for " + this.coordinates
          .getId());
    }
  }

  public FileObject getBundleFile() {
    return bundleFile;
  }

  public BundleCoordinates getCoordinates() {
    return coordinates;
  }

  public BundleCoordinates getDependencyCoordinates() {
    return dependencyCoordinates;
  }

  public String getBuildTag() {
    return buildTag;
  }

  public String getBuildRevision() {
    return buildRevision;
  }

  public String getBuildBranch() {
    return buildBranch;
  }

  public String getBuildTimestamp() {
    return buildTimestamp;
  }

  public String getBuildJdk() {
    return buildJdk;
  }

  public String getBuiltBy() {
    return builtBy;
  }

  @Override
  public String toString() {
    return coordinates.toString();
  }

  public Date getBuildTimestampDate() {
    if (buildTimestamp != null && !buildTimestamp.isEmpty()) {
      try {
        SimpleDateFormat buildTimestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date buildTimestampDate = buildTimestampFormat.parse(buildTimestamp);
        return buildTimestampDate;
      } catch (ParseException parseEx) {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Builder for BundleDetails. The withCoordinates and withBundleFile properties are required
   */
  public static class Builder {

    private FileObject bundleFile;

    private BundleCoordinates coordinates;
    private BundleCoordinates dependencyCoordinates;

    private String buildTag;
    private String buildRevision;
    private String buildBranch;
    private String buildTimestamp;
    private String buildJdk;
    private String builtBy;

    public Builder withBundleFile(final FileObject bundleFile) {
      this.bundleFile = bundleFile;
      return this;
    }

    public Builder withCoordinates(final BundleCoordinates coordinates) {
      this.coordinates = coordinates;
      return this;
    }

    public Builder withDependencyCoordinates(final BundleCoordinates dependencyCoordinates) {
      this.dependencyCoordinates = dependencyCoordinates;
      return this;
    }

    public Builder withBuildTag(final String buildTag) {
      this.buildTag = buildTag;
      return this;
    }

    public Builder withBuildRevision(final String buildRevision) {
      this.buildRevision = buildRevision;
      return this;
    }

    public Builder withBuildBranch(final String buildBranch) {
      this.buildBranch = buildBranch;
      return this;
    }

    public Builder withBuildTimestamp(final String buildTimestamp) {
      this.buildTimestamp = buildTimestamp;
      return this;
    }

    public Builder withBuildJdk(final String buildJdk) {
      this.buildJdk = buildJdk;
      return this;
    }

    public Builder withBuiltBy(final String builtBy) {
      this.builtBy = builtBy;
      return this;
    }

    /**
     * Builds a BundleDetails instance. withBundleFile and withCoordinates are required. An
     * IllegalStateException will result if they are missing on build()
     */
    public BundleDetails build() {
      try {
        if (this.bundleFile == null || this.bundleFile.exists() == false) {
          throw new IllegalStateException("Invalid Bundle File");
        } else if (this.coordinates == null) {
          throw new IllegalStateException("Invalid Coordinates");
        }
        return new BundleDetails(this);
      } catch (FileSystemException e) {
        throw new IllegalStateException(e);
      }
    }
  }

}
