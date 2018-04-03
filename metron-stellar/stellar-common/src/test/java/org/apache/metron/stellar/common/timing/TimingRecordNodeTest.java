/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.timing;

import org.junit.Assert;
import org.junit.Test;


public class TimingRecordNodeTest {

  private static final String PARENT_PATH = "topFunction/levelOneFunction";
  private static final String NODE_NAME = "testFunction";
  private static final String NODE_PATH = String.format("%s/%s", PARENT_PATH, NODE_NAME);
  private static final String TAG_ONE = "tag_one";
  private static final String TAG_TWO = "tag_two";
  private static final String CHILD_NAME = "the child";

  @Test
  public void testNullParentDoesNotThrowException() {
    TimingRecordNode theNode = new TimingRecordNode(null, NODE_NAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullNameThrowsException() {
    TimingRecordNode theNode = new TimingRecordNode(null, null);
  }

  @Test
  public void testGetParentPath() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    Assert.assertEquals(PARENT_PATH, theNode.getParentPath());
  }

  @Test
  public void testGetTimingName() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    Assert.assertEquals(NODE_NAME, theNode.getTimingName());
  }

  @Test
  public void testIsRunning() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    Assert.assertFalse(theNode.isRunning());
    theNode.start();
    Assert.assertTrue(theNode.isRunning());
  }

  @Test
  public void testStart() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.start();
    Assert.assertTrue(theNode.isRunning());
  }

  @Test
  public void testStartStart() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.start();
    theNode.start();
    Assert.assertTrue(theNode.isRunning());
  }

  @Test
  public void testStop() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.start();
    theNode.stop();
    Assert.assertFalse(theNode.isRunning());
  }

  @Test(expected = IllegalStateException.class)
  public void testStopWithRunningChildrenThrowsException() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.start();
    theNode.createChild("child").start();
    theNode.stop();
  }

  @Test
  public void testGetPath() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    Assert.assertEquals(NODE_PATH, theNode.getPath());
  }

  @Test
  public void testGetPathNoParent() {
    TimingRecordNode theNode = new TimingRecordNode(null, NODE_NAME);
    Assert.assertEquals(NODE_NAME, theNode.getPath());
  }

  @Test
  public void testCreateChild() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.start();
    Assert.assertNotNull(theNode.createChild("child"));
  }

  @Test(expected = IllegalStateException.class)
  public void testCreateChildWhenNotStartedThrowsException() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.createChild("child");
  }

  @Test
  public void testChildPath() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.start();
    Assert.assertEquals(theNode.createChild("child").getPath(),
        String.format("%s/%s", NODE_PATH, "child"));
  }

  @Test
  public void testVisit() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    theNode.start();
    theNode.createChild("child");
    theNode.visit(0, new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        if (level == 0) {
          Assert.assertEquals(node.getTimingName(), NODE_NAME);
        } else if (level == 1) {
          Assert.assertEquals(node.getTimingName(), "child");
        } else {
          throw new IllegalStateException("Invalid node level");
        }
      }
    });
  }

  @Test
  public void getStopWatch() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    Assert.assertNotNull(theNode.getStopWatch());
  }

  @Test
  public void getTags() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME, TAG_ONE, TAG_TWO);
    String[] tags = theNode.getTags();
    Assert.assertNotNull(tags);
    Assert.assertEquals(tags.length, 2);
  }

  @Test
  public void getTagsWithNoTags() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME);
    String[] tags = theNode.getTags();
    Assert.assertNotNull(tags);
    Assert.assertEquals(tags.length, 0);
  }

  @Test
  public void getChildren() {
    TimingRecordNode theNode = new TimingRecordNode(PARENT_PATH, NODE_NAME, TAG_ONE, TAG_TWO);
    theNode.start();
    theNode.createChild(CHILD_NAME);
    Iterable<TimingRecordNode> children = theNode.getChildren();
    int count = 0;
    for (TimingRecordNode child : children) {
      count++;
    }
    Assert.assertEquals(count, 1);
  }
}
