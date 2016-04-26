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
package org.apache.metron.test.utils;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;

import java.io.File;
import java.util.Set;
import java.util.Stack;

public class UnitTestHelper {
    public static String findDir(String name) {
        return findDir(new File("."), name);
    }

    public static String findDir(File startDir, String name) {
        Stack<File> s = new Stack<File>();
        s.push(startDir);
        while(!s.empty()) {
            File parent = s.pop();
            if(parent.getName().equalsIgnoreCase(name)) {
                return parent.getAbsolutePath();
            }
            else {
                File[] children = parent.listFiles();
                if(children != null) {
                    for (File child : children) {
                        s.push(child);
                    }
                }
            }
        }
        return null;
    }

    public static <T> void assertSetEqual(String type, Set<T> expectedPcapIds, Set<T> found) {
        boolean mismatch = false;
        for(T f : found) {
            if(!expectedPcapIds.contains(f)) {
                mismatch = true;
                System.out.println("Found " + type + " that I did not expect: " + f);
            }
        }
        for(T expectedId : expectedPcapIds) {
            if(!found.contains(expectedId)) {
                mismatch = true;
                System.out.println("Expected " + type + " that I did not index: " + expectedId);
            }
        }
        Assert.assertFalse(mismatch);
    }

    public static void verboseLogging() {
        verboseLogging("%d [%p|%c|%C{1}] %m%n", Level.ALL);
    }
    public static void verboseLogging(String pattern, Level level) {
        ConsoleAppender console = new ConsoleAppender(); //create appender
        //configure the appender
        console.setLayout(new PatternLayout(pattern));
        console.setThreshold(level);
        console.activateOptions();
        //add appender to any Logger (here is root)
        Logger.getRootLogger().addAppender(console);
    }
}
