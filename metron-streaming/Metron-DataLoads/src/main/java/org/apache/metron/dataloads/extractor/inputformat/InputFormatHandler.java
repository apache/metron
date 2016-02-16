package org.apache.metron.dataloads.extractor.inputformat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import java.io.IOException;
import java.util.Map;

/**
 * Created by cstella on 2/8/16.
 */
public interface InputFormatHandler {
    void set(Job job, Path input, Map<String, Object> config) throws IOException;
}
