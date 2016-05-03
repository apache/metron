package org.apache.metron.test;

public enum TestDataType {

  RAW("raw"),PARSED("parsed"),INDEXED("indexed");

  private String directoryName;
  TestDataType(String directoryName) {
    this.directoryName = directoryName;
  }
  public String getDirectoryName() {
    return directoryName;
  }
}
