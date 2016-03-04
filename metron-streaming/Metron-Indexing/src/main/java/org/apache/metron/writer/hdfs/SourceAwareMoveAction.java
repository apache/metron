package org.apache.metron.writer.hdfs;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.storm.hdfs.common.rotation.RotationAction;

import java.io.IOException;

public class SourceAwareMoveAction implements RotationAction{
  private static final Logger LOG = Logger.getLogger(SourceHandler.class);
  private String destination;

  public SourceAwareMoveAction toDestination(String destDir){
    destination = destDir;
    return this;
  }

  private static String getSource(Path filePath) {
    return filePath.getParent().getName();
  }

  @Override
  public void execute(FileSystem fileSystem, Path filePath) throws IOException {
    Path destPath = new Path(new Path(destination, getSource(filePath)), filePath.getName());
    LOG.info("Moving file " + filePath + " to " + destPath);
    boolean success = fileSystem.rename(filePath, destPath);
    return;
  }
}
