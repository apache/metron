package org.apache.metron.reference.lookup.handler;

import org.apache.metron.reference.lookup.LookupKey;

import java.io.IOException;

/**
 * Created by cstella on 2/5/16.
 */
public interface Handler<CONTEXT_T, KEY_T extends LookupKey, RESULT_T> extends AutoCloseable{
    boolean exists(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException;
    RESULT_T get(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException;
}
