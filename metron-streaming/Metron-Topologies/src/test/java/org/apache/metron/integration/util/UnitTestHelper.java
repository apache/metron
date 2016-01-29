package org.apache.metron.integration.util;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;

import java.io.File;
import java.util.Set;
import java.util.Stack;

/**
 * Created by cstella on 1/28/16.
 */
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
