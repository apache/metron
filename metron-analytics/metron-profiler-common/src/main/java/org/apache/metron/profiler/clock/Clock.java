package org.apache.metron.profiler.clock;

/**
 * A clock can tell time; imagine that.
 *
 * This allows the Profiler to support different treatments of time like wall clock versus event time.
 */
public interface Clock {

  /**
   * The current time in epoch milliseconds.
   */
  long currentTimeMillis();

}
