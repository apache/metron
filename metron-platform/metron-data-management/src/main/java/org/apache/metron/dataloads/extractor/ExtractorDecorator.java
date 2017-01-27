package org.apache.metron.dataloads.extractor;

import org.apache.metron.enrichment.lookup.LookupKV;

import java.io.IOException;
import java.util.Map;

public class ExtractorDecorator implements Extractor {

  protected final Extractor decoratedExtractor;

  public ExtractorDecorator(Extractor decoratedExtractor) {
    this.decoratedExtractor = decoratedExtractor;
  }

  @Override
  public Iterable<LookupKV> extract(String line) throws IOException {
    return decoratedExtractor.extract(line);
  }

  @Override
  public void initialize(Map<String, Object> config) {
    decoratedExtractor.initialize(config);
  }
}
