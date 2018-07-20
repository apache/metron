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

package org.apache.metron.pcap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

public class PcapPagesTest {

  @Test
  public void iterates_paths() {
    Path path1 = new Path("/1.txt");
    Path path2 = new Path("/2.txt");
    Path path3 = new Path("/3.txt");
    List<Path> paths = new ArrayList<>();
    paths.add(path1);
    paths.add(path2);
    paths.add(path3);
    PcapPages pages = new PcapPages(paths);
    assertThat("Wrong num pages.", pages.getSize(), equalTo(3));

    for (int i = 0; i < pages.getSize(); i++) {
      assertThat("Page should be equal", pages.getPage(i).toString(),
          equalTo(paths.get(i).toString()));
    }

  }

  @Test
  public void clones_with_copy_constructor() {
    Path path1 = new Path("/1.txt");
    Path path2 = new Path("/2.txt");
    Path path3 = new Path("/3.txt");
    List<Path> paths = new ArrayList<>();
    paths.add(path1);
    paths.add(path2);
    paths.add(path3);

    PcapPages pages = new PcapPages(paths);
    PcapPages clonedPages = new PcapPages(pages);
    assertThat(clonedPages, notNullValue());
    assertThat(clonedPages.getSize(), equalTo(3));
    assertThat(clonedPages, not(sameInstance(pages)));

    for (int i = 0; i < pages.getSize(); i++) {
      assertThat("Page should be different instance.", pages.getPage(i),
          not(sameInstance(clonedPages.getPage(i))));
      assertThat("Page should be same path.", pages.getPage(i), equalTo(clonedPages.getPage(i)));
    }
  }

}
