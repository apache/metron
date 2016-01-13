package com.apache.metron.pcap;

public class IEEE_802_1Q {

	  int priorityCodePoint = 0;
	  int dropEligibleIndicator = 0;
	  int vLANIdentifier = 0;

	  public IEEE_802_1Q(int priorityCodePoint, int dropEligibleIndicator,
	      int vLANIdentifier) {
	    this.priorityCodePoint = priorityCodePoint;
	    this.dropEligibleIndicator = dropEligibleIndicator;
	    this.vLANIdentifier = vLANIdentifier;
	  }

	  public int getPriorityCodePoint() {
	    return priorityCodePoint;
	  }

	  public int getDropEligibleIndicator() {
	    return dropEligibleIndicator;
	  }

	  public int getvLANIdentifier() {
	    return vLANIdentifier;
	  }
	}