package com.apache.metron.hbase;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.HTable;

import backtype.storm.generated.GlobalStreamId;
import backtype.storm.grouping.CustomStreamGrouping;
import backtype.storm.task.WorkerTopologyContext;

public class HBaseStreamPartitioner implements CustomStreamGrouping {

  private static final long serialVersionUID = -148324019395976092L;
  private String[] regionStartKeys = { "0" };
  private Map<String, String> regionStartKeyRegionNameMap = new HashMap<String, String>();

  private List<Integer> targetTasks = null;
  private int targetTasksSize = 0;
  private int rowKeyFieldIndex = 0;
  private String tableName = null;
  private long regionCheckTime = 0;
  private int regionInforRefreshIntervalInMins = 60;
  private int regionInforRefreshIntervalInMillis = regionInforRefreshIntervalInMins * 60000;

  HTable hTable = null;;

  
  public void prepare(WorkerTopologyContext context, GlobalStreamId stream, List<Integer> targetTasks) {
    
    System.out.println("preparing HBaseStreamPartitioner for streamId " + stream.get_streamId());
    this.targetTasks = targetTasks;
    this.targetTasksSize = this.targetTasks.size();

    Configuration conf = HBaseConfiguration.create();
    try {
      hTable = new HTable(conf, tableName);
      refreshRegionInfo(tableName);

      System.out.println("regionStartKeyRegionNameMap: " + regionStartKeyRegionNameMap);

    } catch (IOException e) {
      e.printStackTrace();
    }

  }
  
  public void prepare() {
    
    System.out.println("preparing HBaseStreamPartitioner for streamId " );

    Configuration conf = HBaseConfiguration.create();
    try {
      hTable = new HTable(conf, tableName);
      refreshRegionInfo(tableName);

      System.out.println("regionStartKeyRegionNameMap: " + regionStartKeyRegionNameMap);

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public HBaseStreamPartitioner(String tableName, int rowKeyFieldIndex, int regionInforRefreshIntervalInMins) {
    System.out.println("Created HBaseStreamPartitioner ");
    this.rowKeyFieldIndex = rowKeyFieldIndex;
    this.tableName = tableName;
    this.regionInforRefreshIntervalInMins = regionInforRefreshIntervalInMins;
    this.regionInforRefreshIntervalInMillis = regionInforRefreshIntervalInMins * 60000;

  }

  
  public List<Integer> chooseTasks(int taskId, List<Object> values) {
    List<Integer> choosenTasks = null;
    System.out.println("Choosing task for taskId " + taskId + " and values " + values);

    if (regionInforRefreshIntervalInMillis > (System.currentTimeMillis() - regionCheckTime)) {
      try {
        refreshRegionInfo(tableName);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    int regionIndex = getRegionIndex((String) values.get(rowKeyFieldIndex));

    if (regionIndex < targetTasksSize) {
      choosenTasks = Arrays.asList(regionIndex);

    } else {
      choosenTasks = Arrays.asList(regionIndex % targetTasksSize);
    }
    System.out.println("Choosen tasks are " + choosenTasks);

    return choosenTasks;


  }

  
  
  public int getRegionIndex(String key) {
    int index = Arrays.binarySearch(regionStartKeys, key);
    if (index < -1) {
      index = (index + 2) * -1;
    } else if (index == -1) {
      index = 0;
    }

    return index;
  }

  private void refreshRegionInfo(String tableName) throws IOException {

    System.out.println("in refreshRegionInfo ");

    Map<HRegionInfo, ServerName> regionMap = hTable.getRegionLocations();

    synchronized (regionStartKeys) {
      synchronized (regionStartKeyRegionNameMap) {
        regionStartKeys = new String[regionMap.size()];
        int index = 0;
        String startKey = null;
        regionStartKeyRegionNameMap.clear();
        for (HRegionInfo regionInfo : regionMap.keySet()) {
          startKey = new String(regionInfo.getStartKey());
          regionStartKeyRegionNameMap.put(startKey, regionInfo.getRegionNameAsString());
          regionStartKeys[index] = startKey;
          index++;
        }

        Arrays.sort(regionStartKeys);
        regionCheckTime = System.currentTimeMillis();
      }
    }
  }
}