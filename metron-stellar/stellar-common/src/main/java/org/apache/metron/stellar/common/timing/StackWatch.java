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

package org.apache.metron.stellar.common.timing;

import java.util.Deque;
import java.util.LinkedList;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.lang3.StringUtils;

/**
 * The StackWatch class, along with the {@code TimeRecordNode} provide a wrapper around the
 * Apache Commons StopWatch class.
 * <p>
 * While the {@code StopWatch} provides functionality to time the length of operations, there is no
 * context or name to go with the time tracked.
 * </p>
 * It is not possible to time nested calls with the {@code StopWatch} as well.
 * <p>
 * This class provides that functionality, allowing successive calls to startTime to track nested
 * calls.
 * </p>
 * <p>
 * Each start provides a name, thus providing context to the timing.
 * </p>
 * At the end of a timing 'run', a visitor interface provides the ability to visit all the timing
 * 'nodes' and capture their output.
 * <p>
 * The {@code TimeRecordNodes} provide a tree structure in support of nesting.
 * A {@code Deque} is use to track the current time node.
 * </p>
 *
 * <p>
 *   This class is not thread safe, and is meant to track timings across multiple calls on the same
 *   thread
 * </p>
 */
@NotThreadSafe
public class StackWatch {

  // the Deque use to track the current operation
  private Deque<TimeRecordNode> deque = new LinkedList<>();
  // the root name
  private String rootName;
  // the root node
  private TimeRecordNode rootNode;

  /**
   * Constructor with root name.
   * If the passed name is empty, 'root' will be used.
   * @param name the root name
   */
  public StackWatch(String name) {
    if (StringUtils.isEmpty(name)) {
      this.rootName = "root";
    }
    this.rootName = name;
  }

  /**
   * Start a named timing.
   * This may be called multiple times, before a stopTime() call is made, if calls are nested
   * @param name the name of this timing
   */
  public void startTime(String name, String... tags) {
    // if the deque is empty, then this is a child of root
    TimeRecordNode parentNode = null;
    if (deque.isEmpty()) {
      // create, add, and start the root node
      rootNode = new TimeRecordNode(null, rootName);
      deque.push(rootNode);
      parentNode = rootNode;
      parentNode.start();
    } else {
      // if the current node is not running, then this is an InvalidStateException
      if (deque.peek().isRunning()) {
        // this is a nested start, add as child to current running
        parentNode = deque.peek();
      } else {
        throw new IllegalStateException(
            String.format("Node on queue is not running: %s", deque.peek().getPath()));
      }
    }

    // request the current node to create a new child with this timing name and start it
    TimeRecordNode node = parentNode.createChild(name, tags);
    node.start();
    // this node is now top of the stack
    deque.push(node);
  }

  /**
   * Stop the current timing.
   * In the case of nested timings, the current timing is stopped and removed from the {@code Deque}
   * causing the parent node to be the top of the stack
   */
  public void stopTime() {
    if (deque.isEmpty()) {
      throw new IllegalStateException("Trying to stop time, there are no running records in deque");
    }
    deque.pop().stop();
  }

  /**
   * Stops all current timings
   */
  public void stopWatch() {
    deque.forEach(TimeRecordNode::stop);
  }

  /**
   * Clears the queue and the root node.
   */
  public void close() {
    deque.clear();
    rootNode = null;
  }

  /**
   * Initiate the visitation of the nodes in this timing.
   * The {@code TimeRecordNodeVisitor} will be called back for each node in the tree.
   * @param visitor callback interface.
   */
  public void visit(TimeRecordNodeVisitor visitor) {
    rootNode.visit(0, visitor);
  }
}
