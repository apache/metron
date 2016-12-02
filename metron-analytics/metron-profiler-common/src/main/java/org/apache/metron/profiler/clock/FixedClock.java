package org.apache.metron.profiler.clock;

/**
 * A clock that reports whatever time you tell it to.  Most useful for testing.
 */
public class FixedClock implements Clock {

  private long epochMillis;

  public void setTime(long epochMillis) {
    this.epochMillis = epochMillis;
  }

  @Override
  public long currentTimeMillis() {
    return this.epochMillis;
  }
}
