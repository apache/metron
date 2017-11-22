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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;

/**
 * BundleSelector implements the {@link FileSelector} interface and is used
 * where required to select bundle files.
 */
public class BundleSelector implements FileSelector {

  private String archiveExtension;

  public BundleSelector() {
    this(BundleProperties.DEFAULT_ARCHIVE_EXTENSION);
  }

  /**
   * Constructor.
   * @param archiveExtension override the default archive extension
   */
  public BundleSelector(String archiveExtension) {
    if (StringUtils.isEmpty(archiveExtension)) {
      this.archiveExtension = BundleProperties.DEFAULT_ARCHIVE_EXTENSION;
    }
    this.archiveExtension = archiveExtension;
  }

  @Override
  public boolean includeFile(FileSelectInfo fileSelectInfo) throws Exception {
    final String nameToTest = fileSelectInfo.getFile().getName().getExtension();
    return nameToTest.equals(archiveExtension) && fileSelectInfo.getFile().isFile();
  }

  @Override
  public boolean traverseDescendents(FileSelectInfo fileSelectInfo) throws Exception {
    return true;
  }
}
