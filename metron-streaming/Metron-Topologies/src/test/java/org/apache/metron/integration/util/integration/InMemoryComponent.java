package org.apache.metron.integration.util.integration;

/**
 * Created by cstella on 1/28/16.
 */
public interface InMemoryComponent {
    public void start() throws UnableToStartException;
    public void stop();
}
