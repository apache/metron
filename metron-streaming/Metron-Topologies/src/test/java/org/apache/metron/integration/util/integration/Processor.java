package org.apache.metron.integration.util.integration;

/**
 * Created by cstella on 1/28/16.
 */
public interface Processor<T> {
    ReadinessState process(ComponentRunner runner);
    T getResult();
}
