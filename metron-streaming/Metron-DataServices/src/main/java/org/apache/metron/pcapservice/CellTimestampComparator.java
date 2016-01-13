package com.apache.metron.pcapservice;

import java.util.Comparator;

import org.apache.hadoop.hbase.Cell;

/**
 * Comparator created for sorting pcaps cells based on the timestamp (asc).
 * 
 * @author Sayi
 */
public class CellTimestampComparator implements Comparator<Cell> {

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  
  public int compare(Cell o1, Cell o2) {
    return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
  }
}
