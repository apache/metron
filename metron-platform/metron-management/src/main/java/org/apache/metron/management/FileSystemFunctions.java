package org.apache.metron.management;

import com.jakewharton.fliptables.FlipTable;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.log4j.Logger;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.utils.ConversionUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

public class FileSystemFunctions {
  private static final Logger LOG = Logger.getLogger(FileSystemFunctions.class);

  private static abstract class AbstractFileSystemFunction implements StellarFunction {
    protected FileSystem fs;

    @Override
    public void initialize(Context context) {
      try {
        fs = getFs();
      } catch (IOException e) {
        String message = "Unable to get FileSystem: " + e.getMessage();
        LOG.error(message, e);
        throw new IllegalStateException(message, e);
      }
    }

    @Override
    public boolean isInitialized() {
      return fs != null;
    }

    public abstract FileSystem getFs() throws IOException;
  }

  private static abstract class FileSystemGet extends AbstractFileSystemFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String path = (String) args.get(0);
      if(path == null) {
        return null;
      }
      try(FSDataInputStream is = fs.open(new Path(path))) {
        return IOUtils.toString(is);
      } catch (IOException e) {
        String message = "Unable to read " + path + ": " + e.getMessage();
        LOG.error(message, e);
        return null;
      }
    }
  }

  public static abstract class FileSystemRm extends AbstractFileSystemFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      String path = (String) args.get(0);
      if(path == null) {
        return false;
      }

      boolean recursive = false;
      if(args.size() > 1) {
        recursive = ConversionUtils.convert(args.get(1), Boolean.class);
      }

      try {
        fs.delete(new Path(path), recursive);
        return true;
      } catch (IOException e) {
        String message = "Unable to remove " + path + (recursive?" recursively":"") + ": " + e.getMessage();
        LOG.error(message, e);
        return false;
      }
    }
  }

  public static abstract class FileSystemPut extends AbstractFileSystemFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String content = (String)args.get(0);
      if(content == null) {
        return false;
      }
      String path = (String) args.get(1);
      if(path == null) {
        return false;
      }

      try(FSDataOutputStream os = fs.create(new Path(path))) {
        os.writeChars(content);
        os.flush();
        return true;
      } catch (IOException e) {
        String message = "Unable to write " + path + ": " + e.getMessage();
        LOG.error(message, e);
        return false;
      }
    }
  }

  public static abstract class FileSystemLs extends AbstractFileSystemFunction {
    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {

      @Override
      protected DateFormat initialValue() {
        return DateFormat.getDateTimeInstance(
                DateFormat.DEFAULT,
                DateFormat.DEFAULT,
                Locale.getDefault()
                );
      }
    };

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      String path = (String) args.get(1);
      if(path == null) {
        return false;
      }
      try {
        String[] headers = new String[] {"PERMISSION", "OWNER", "GROUP", "SIZE", "LAST MOD TIME", "NAME"};
        List<String[]> dataList = new ArrayList<>();
        for(RemoteIterator<LocatedFileStatus> it = fs.listFiles(new Path(path), false);it.hasNext();) {
          final FileStatus status = it.next();
          dataList.add(new String[] {
            status.getPermission().toString()
           ,status.getOwner()
           ,status.getGroup()
           ,status.getLen() + ""
           ,dateFormat.get().format(new Date(status.getModificationTime()))
           ,status.getPath().getName()
          });
        }
        Collections.sort(dataList, (o1, o2) -> {
            try {
              Date left = dateFormat.get().parse(o1[4]);
              Date right = dateFormat.get().parse(o2[4]);
              int ret = left.compareTo(right);
              //first sort by date, then by name.
              if(ret == 0) {
                return o1[5].compareTo(o2[5]);
              }
              else {
                return ret;
              }
            } catch (java.text.ParseException e) {
              String message = "Unable to parse " + o1 + " or " + o2 + ": " + e.getMessage();
              LOG.error(message, e);
              throw new IllegalStateException(message, e);
            }
        });
        String[][] data = new String[dataList.size()][headers.length];
        for(int i = 0;i < dataList.size();++i) {
          data[i] = dataList.get(i);
        }
        return FlipTable.of(headers, data);
      } catch (IOException e) {
        String message = "Unable to list" + path + ": " + e.getMessage();
        LOG.error(message, e);
        return null;
      }
    }
  }

  @Stellar(namespace="FILE"
          ,name="RM"
          ,description="Removes the path"
          ,params = { "path - The path of the file."
                    , "recursive - Recursively remove or not (optional and defaulted to false)"
                    }
          ,returns = "boolean - true if successful, false otherwise."

  )
  public static class FileRm extends FileSystemRm {

    @Override
    public FileSystem getFs() throws IOException {
      return new LocalFileSystem();
    }
  }

  @Stellar(namespace="HDFS"
          ,name="RM"
          ,description="Removes the path"
          ,params = { "path - The path in HDFS of the file."
                    , "recursive - Recursively remove or not (optional and defaulted to false)"
                    }
          ,returns = "boolean - true if successful, false otherwise."

  )
  public static class HDFSRm extends FileSystemRm {

    @Override
    public FileSystem getFs() throws IOException {
      return FileSystem.get(new Configuration());
    }
  }

  @Stellar(namespace="FILE"
          ,name="LS"
          ,description="Lists the contents of a directory"
          ,params = { "path - The path of the file."
                    }
          ,returns = "The contents of the directory in tabular form sorted by last modification date."

  )
  public static class FileLs extends FileSystemLs {

    @Override
    public FileSystem getFs() throws IOException {
      return new LocalFileSystem();
    }
  }

  @Stellar(namespace="HDFS"
          ,name="LS"
          ,description="Lists the contents of a directory"
          ,params = { "path - The path in HDFS of the file."
                    }
          ,returns = "The contents of the directory in tabular form sorted by last modification date."

  )
  public static class HDFSLs extends FileSystemLs {

    @Override
    public FileSystem getFs() throws IOException {
      return FileSystem.get(new Configuration());
    }
  }

  @Stellar(namespace="HDFS"
          ,name="PUT"
          ,description="Writes the contents a string to a file in HDFS."
          ,params = { "content - The content to write out"
                    , "path - The path in HDFS of the file."
                    }
          ,returns = "true if the file was written and false otherwise."

  )
  public static class HDFSPut extends FileSystemPut {

    @Override
    public FileSystem getFs() throws IOException {
      return FileSystem.get(new Configuration());
    }
  }

  @Stellar(namespace="FILE"
          ,name="PUT"
          ,description="Writes the contents a string to a local file."
          ,params = { "content - The content to write out"
                    , "path - The path of the file."
                    }
          ,returns = "true if the file was written and false otherwise."

  )
  public static class FilePut extends FileSystemPut {

    @Override
    public FileSystem getFs() throws IOException {
      return new LocalFileSystem();
    }
  }

  @Stellar(namespace="HDFS"
          ,name="GET"
          ,description="Retrieves the contents as a string of a file in HDFS."
          ,params = { "path - The path in HDFS of the file."}
          ,returns = "The contents of the file in the path from HDFS and null otherwise."

  )
  public static class HDFSGet extends FileSystemGet {

    @Override
    public FileSystem getFs() throws IOException {
      return FileSystem.get(new Configuration());
    }
  }

  @Stellar(namespace="FILE"
          ,name="GET"
          ,description="Retrieves the contents as a string of a file on the local filesystem."
          ,params = { "path - The path of the file."}
          ,returns = "The contents of the file or null otherwise."

  )
  public static class FileGet extends FileSystemGet {

    @Override
    public FileSystem getFs() throws IOException {
      return new LocalFileSystem();
    }
  }

}
