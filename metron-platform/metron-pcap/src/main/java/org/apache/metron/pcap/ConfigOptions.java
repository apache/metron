package org.apache.metron.pcap;

import org.apache.hadoop.fs.Path;

import java.util.function.BiFunction;

public enum ConfigOptions implements ConfigOption {
  PREFIX("prefix"),
  FINAL_FILENAME_PREFIX("finalFilenamePrefix"),
  JOB_NAME("jobName"),
  FINAL_OUTPUT_PATH("finalOutputPath"),
  BASE_PATH("basePath", (s,o) ->  o == null?null:new Path(o.toString())),
  INTERRIM_RESULT_PATH("interimResultPath", (s,o) ->  o == null?null:new Path(o.toString())),
  NUM_REDUCERS("numReducers"),
  START_TIME("startTime"),
  END_TIME("endTime"),
  START_TIME_NS("startNs"),
  END_TIME_NS("endNs"),
  NUM_RECORDS_PER_FILE("numRecordsPerFile"),
  FIELDS("fields"),
  FILTER_IMPL("filterImpl"),
  HADOOP_CONF("hadoopConf"),
  FILESYSTEM("fileSystem")
  ;

  public static final BiFunction<String, Object, Path> STRING_TO_PATH = (s,o) ->  o == null?null:new Path(o.toString());
  String key;
  BiFunction<String, Object, Object> transform = (s, o) -> o;

  ConfigOptions(String key) {
    this.key = key;
  }
  ConfigOptions(String key, BiFunction<String, Object, Object> transform ) {
    this.key = key;
    this.transform = transform;
  }


  @Override
  public String getKey() {
    return key;
  }

  @Override
  public BiFunction<String, Object, Object> transform() {
    return transform;
  }
}
