package org.apache.metron.dataloads.nonbulk.flatfile.location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class URLLocation implements RawLocation {

  @Override
  public Optional<List<String>> list(String loc) throws IOException {
    return Optional.of(Collections.emptyList());
  }

  @Override
  public boolean exists(String loc) throws IOException {
    return true;
  }

  @Override
  public boolean isDirectory(String loc) throws IOException {
    return false;
  }

  @Override
  public BufferedReader openReader(String loc) throws IOException {
    InputStream is = new URL(loc).openStream();
    if(loc.endsWith(".zip")) {
      is = new ZipInputStream(is);
    }
    else if(loc.endsWith(".gz")) {
      is = new GZIPInputStream(is);
    }
    return new BufferedReader(new InputStreamReader(is));
  }

  @Override
  public boolean match(String loc) {
    try {
      new URL(loc);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }
}
