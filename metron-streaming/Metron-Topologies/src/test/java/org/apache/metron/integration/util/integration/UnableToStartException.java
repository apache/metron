package org.apache.metron.integration.util.integration;

/**
 * Created by cstella on 1/28/16.
 */
public class UnableToStartException extends Exception {
    public UnableToStartException(String message) {
        super(message);
    }
    public UnableToStartException(String message, Throwable t) {
        super(message, t);
    }
}
