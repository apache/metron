package org.apache.metron.reference.lookup;

import org.apache.metron.reference.lookup.accesstracker.AccessTracker;
import org.apache.metron.reference.lookup.handler.Handler;

import java.io.IOException;

/**
 * Created by cstella on 2/5/16.
 */
public class Lookup<CONTEXT_T, KEY_T extends LookupKey, RESULT_T> implements Handler<CONTEXT_T, KEY_T, RESULT_T> {
    private String name;
    private AccessTracker accessTracker;
    private Handler<CONTEXT_T, KEY_T, RESULT_T> lookupHandler;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccessTracker getAccessTracker() {
        return accessTracker;
    }

    public void setAccessTracker(AccessTracker accessTracker) {
        this.accessTracker = accessTracker;
    }

    public Handler< CONTEXT_T, KEY_T, RESULT_T > getLookupHandler() {
        return lookupHandler;
    }

    public void setLookupHandler(Handler< CONTEXT_T, KEY_T, RESULT_T > lookupHandler) {
        this.lookupHandler = lookupHandler;
    }

    @Override
    public boolean exists(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException {
        if(logAccess) {
            accessTracker.logAccess(key);
        }
        return lookupHandler.exists(key, context, logAccess);
    }

    @Override
    public RESULT_T get(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException {
        if(logAccess) {
            accessTracker.logAccess(key);
        }
        return lookupHandler.get(key, context, logAccess);
    }

    @Override
    public void close() throws Exception {
        accessTracker.cleanup();
        lookupHandler.close();
    }
}
