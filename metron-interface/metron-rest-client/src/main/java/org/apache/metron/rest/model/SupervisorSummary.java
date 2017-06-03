package org.apache.metron.rest.model;

import java.util.Arrays;

public class SupervisorSummary {

  private SupervisorStatus[] supervisors;

  public SupervisorSummary(){}
  public SupervisorSummary(SupervisorStatus[] supervisors) {
    this.supervisors = supervisors;
  }

  public SupervisorStatus[] getSupervisors() {
    return supervisors;
  }

  public void setSupervisors(SupervisorStatus[] supervisors) {
    this.supervisors = supervisors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SupervisorSummary that = (SupervisorSummary) o;

    return supervisors != null ? Arrays.equals(supervisors, that.supervisors) : that.supervisors != null;
  }

  @Override
  public int hashCode() {
    return supervisors != null ? Arrays.hashCode(supervisors) : 0;
  }
}
