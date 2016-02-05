package org.apache.metron.reference.lookup.accesstracker;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by cstella on 2/5/16.
 */
public enum Util {
    INSTANCE;
    public static class PathFilter implements org.apache.hadoop.fs.PathFilter {
        Predicate<Path> predicate;
        public PathFilter(long earliest) {
            predicate = new PathPredicate(earliest);
        }
        public PathFilter(Predicate<Path> predicate) {
            this.predicate = predicate;
        }
        /**
         * Tests whether or not the specified abstract pathname should be
         * included in a pathname list.
         *
         * @param path The abstract pathname to be tested
         * @return <code>true</code> if and only if <code>pathname</code>
         * should be included
         */
        @Override
        public boolean accept(Path path) {
            return predicate.apply(path);
        }
    }
    public static class PathPredicate implements Predicate<Path> {
        long earliestTs;
        public PathPredicate(long earliest) {
            earliestTs = earliest;
        }
        @Override
        public boolean apply(@Nullable Path path) {
            String nameSansSuffix = Iterables.getFirst(Splitter.on('.').split(path.getName()), null);
            String timestampStr = Iterables.getLast(Splitter.on('_').split(nameSansSuffix));
            return (Long.parseLong(timestampStr) > earliestTs);
        }
    }



    public AccessTracker loadTracker(FileSystem fs, Path path) throws IOException, ClassNotFoundException {
        ObjectInputStream ois  = null;
        try {
            ois = new ObjectInputStream(fs.open(path));

            return (AccessTracker) (ois.readObject());
        }
        finally {
            if(ois != null) {
                ois.close();
            }
        }
    }

    public void persistTracker(FileSystem fs, Path path, AccessTracker tracker) throws IOException {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(fs.create(path, true));
            oos.writeObject(tracker);
        }
        finally{
            if(oos != null) {
                oos.close();
            }
        }
    }

    public AccessTracker loadAll(FileSystem fs, Path trackerPath, long earliest) throws IOException, ClassNotFoundException {
        AccessTracker tracker = null;
        for(FileStatus status : fs.listStatus(trackerPath, new PathFilter(earliest)) ) {
            if(tracker == null) {
                tracker = loadTracker(fs, status.getPath());
            }
            else {
                tracker = tracker.union(loadTracker(fs, status.getPath()));
            }
        }
        return tracker;
    }
}
