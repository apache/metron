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
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * The {@code StackWatch}, provides a wrapper around the {@code StopWatch} for creating multiple and
 * possibly nested named timings.
 * </p>
 * <p>
 * While the {@code StopWatch} provides functionality to time the length of operations, there is no
 * context or name to go with the time tracked. It is also not possible to time nested calls with
 * the {@code StopWatch}.
 * </p>
 * <p>
 * {@code StackWatch} provides that functionality, allowing successive calls to {@link StackWatch#startTiming(String, String...)} to track
 * nested calls.
 * </p>
 * <p>
 * Each start provides a timing name and a parent timing name, thus providing context to the timing.
 * </p>
 * <p>
 * At the end of a timing 'run', a visitor interface provides the ability to visit all the timing
 * 'nodes' and capture their output, including the level of the call if nested.
 * </p>
 * <p>
 * The {@code TimeRecordNodes} provide a tree structure in support of nesting.
 * A {@code Deque} is use to track the current time node.
 * </p>
 *
 * <pre>
 *   {@code
 *    private void outerFunction() {
 *      try {
 *        StackWatch watch = new StackWatch("OuterFunction");
 *        watch.start();
 *        functionOne();
 *        watch.stop();
 *        watch.visit(new TimingRecordNodeVisitor() {
 *          {@literal @}Override
 *          public void visitRecord(int level, TimingRecordNode node) {
 *            ...
 *          }
 *        });
 *      } catch (Exception e){}
 *    }
 *    private void functionOne(StackWatch watch) throws Exception {
 *      watch.startTiming("One", "OneFunc");
 *      functionOneOne(watch);
 *      watch.stopTiming();
 *    }
 *
 *    private void functionOneOne(StackWatch watch) throws Exception {
 *      watch.startTiming("OneOne", "OneFunc");
 *      functionOneTwo(watch);
 *      watch.stopTiming();
 *    }
 *
 *    private void functionOneTwo(StackWatch watch) throws Exception {
 *      watch.startTiming("OneTwo", "OneFunc");
 *      watch.stopTiming();
 *    }
 *   }
 * </pre>
 *
 *
 * <p>
 *   This class is not thread safe, and is meant to track timings across multiple calls on the same
 *   thread
 * </p>
 *
 */
public class StackWatch {

  /**
   * The default name for the root level timing if not provided
   */
  public static final String DEFAULT_ROOT_NAME = "ROOT_TIMING";

  /**
   * The Deque used to track the timings
   */
  private Deque<TimingRecordNode> deque = new LinkedList<>();

  /**
   * The name of the root node
   */
  private String rootName = DEFAULT_ROOT_NAME;

  /**
   * The root {@code TimingRecordNode}.
   * The root node represents the root of a tree structure, and contains all {@code TimingRecordNode}
   * instances.
   */
  private TimingRecordNode rootNode;

  /**
   * <p>
   * Constructor
   * </p>
   * <p>
   *   The top most timing will be created with the rootName on {@link StackWatch#start()} ()}
   * </p>
   * If the passed name is empty, the DEFAULT_ROOT_NAME {@value DEFAULT_ROOT_NAME} will be used.
   * @param rootName the root name
   */
  public StackWatch(String rootName) {
    if (!StringUtils.isEmpty(rootName)) {
      this.rootName = rootName;
    }
  }

  /**
   * <p>
   *   Starts the {@code StackWatch}.
   * </p>
   * <p>
   *   A root timing will be created named for the rootName and started.
   * </p>
   * <p>
   *   If not called before the first {@link  StackWatch#startTiming(String, String...)} call, then the {@code StackWatch} will
   *   be started at that time.
   * </p>
   * @throws IllegalStateException if the {@code StackWatch} has already been started.
   */
  public void start() {
    if(rootNode != null) {
      throw new IllegalStateException("StackWatch has already been started");
    }
    rootNode = new TimingRecordNode(null, rootName);
    rootNode.start();
    deque.push(rootNode);
  }

  /**
   * <p>
   * Start a timing.  If {@link StackWatch#start()} has not been called, the {@code StackWatch} will be
   * started.
   * </p>
   * <p>
   * This may be called multiple times, before a {@link StackWatch#stopTiming()} call is made, if calls are nested,
   * for example:
   * </p>
   * <pre>
   *   {@code
   *    private void functionOne(StackWatch watch) throws Exception {
   *      watch.startTiming("One", "OneFunc");
   *      functionOneOne(watch);
   *      watch.stopTiming();
   *    }
   *
   *    private void functionOneOne(StackWatch watch) throws Exception {
   *      watch.startTiming("OneOne", "OneFunc");
   *      functionOneTwo(watch);
   *      watch.stopTiming();
   *    }
   *
   *    private void functionOneTwo(StackWatch watch) throws Exception {
   *      watch.startTiming("OneTwo", "OneFunc");
   *      watch.stopTiming();
   *    }
   *   }
   * </pre>
   *
   * <p>
   *   Starting a timing, when it's parent timing is not running results in an
   *   {@code IllegalStateException}.
   * </p>
   * <p>
   *   For example, this code, although contrived, would throw an {@code IllegalStateException}, because
   *   functionOne is not running:
   * </p>
   * <pre>
   *   {@code
   *    private void functionOne(StackWatch watch) throws Exception {
   *      watch.startTiming("One", "OneFunc");
   *      watch.visit(new TimingRecordNodeVisitor() {
   *        {@literal @}Override
   *        public void visitRecord(int level, TimingRecordNode node) {
   *          if(level == 0) {
   *            node.getStopWatch().stop();
   *          }
   *        }
   *      });
   *      functionOneOne(watch);
   *    }
   *
   *    private void functionOneOne(StackWatch watch) throws Exception {
   *      watch.startTiming("OneOne", "OneFunc");
   *      functionOneTwo(watch);
   *      watch.stopTiming();
   *    }
   *   }
   * </pre>
   *
   * <p>
   *   Starting a timing, when some number of timings have been started and all closed results in an
   *   {@code IllegalStateException}.
   * </p>
   * <p>
   *   For example:
   * </p>
   * <pre>
   *   {@code
   *
   *    StackWatch watch = new StackWatch("testStackWatch");
   *    watch.startTiming("Test");
   *    functionOne(watch);
   *    watch.stopTiming();
   *    watch.startTiming("More Test");
   *
   *   }
   * </pre>
   *
   *
   * @param name the name of this timing
   * @param tags the tags to associate with this timing
   * @throws IllegalStateException if the parent timing is not running or there is an attempt to start
   * a new timing after creating a number of timings and closing them all.
   */
  public void startTiming(String name, String... tags) {
    // If the deque is empty, then the root needs to be added and started, unless it already exists.
    // This means that all the timings where closed and a new timing was started.
    // If this happens, it is an IllegalStateException
    TimingRecordNode parentNode = null;
    if (deque.isEmpty()) {
      // create, add, and start the root node
      if(rootNode == null) {
        start();
        parentNode = rootNode;
      } else {
        throw new IllegalStateException("Attempting to start a second set of timings, StackWatch must" +
            " be cleared first");
      }
    } else {
      // if the current node is not running, then this is an InvalidStateException, as the parent
      // cannot close before it's children
      if (deque.peek().isRunning()) {
        // this is a nested start, add as child to current running
        parentNode = deque.peek();
      } else {
        throw new IllegalStateException(
            String.format("Parent TimingRecordNode %s is not running", deque.peek().getPath()));
      }
    }

    // request the current node to create a new child with this timing name and start it
    TimingRecordNode node = parentNode.createChild(name, tags);
    node.start();
    // this node is now top of the stack
    deque.push(node);
  }

  /**
   * <p>
   *  Stop the current timing.
   * </p>
   * <p>
   *  In the case of nested timings, the current timing is stopped and removed from the {@code Deque}
   *  causing the parent node to be the top of the stack.
   * </p>
   * <p>
   *  If the timing being stopped has running child timings an {@code IllegalStateException} will
   *  be thrown.
   * </p>
   * @throws IllegalStateException if stopping a timing with running child timings
   */
  public void stopTiming() {
    if (deque.isEmpty()) {
      throw new IllegalStateException("Trying to stop time, there are no running records in deque");
    }
    deque.pop().stop();
  }

  /**
   * Stops the {@code StackWatch}.
   * @throws IllegalStateException if there are running timings other than the root timing
   */
  public void stop() {
    if(deque.size() > 1) {
      throw new IllegalStateException("Stopping with running timings");
    }
    stopTiming();
  }

  /**
   * Clears the stack and the root node.
   */
  public void clear() {
    deque.clear();
    rootNode = null;
  }

  /**
   * <p>
   * Initiate the visitation of the nodes in this timing.
   * </p>
   * <p>
   * The {@code TimingRecordNodeVisitor} will be called back for each node in the tree, will be
   * passed the level of the node in the tree.  The root level is 0.
   * </p>
   * @param visitor callback interface.
   */
  public void visit(TimingRecordNodeVisitor visitor) {
    rootNode.visit(0, visitor);
  }
}
