package com.opensoc.alerts.server;

public class AlertsFilterCacheEntry {
	

	public String sourceData;
	public long storedAtTime;

	
	public AlertsFilterCacheEntry(String sourceData, long timeNow) {
		this.sourceData = sourceData;
		this.storedAtTime = timeNow;
	}
	
	
	
}
