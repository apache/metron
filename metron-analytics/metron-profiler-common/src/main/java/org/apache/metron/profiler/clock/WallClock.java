package org.apache.metron.profiler.clock;

/**
 * A clock that uses the system clock to provide wall clock time.
 */
public class WallClock implements Clock {

  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
