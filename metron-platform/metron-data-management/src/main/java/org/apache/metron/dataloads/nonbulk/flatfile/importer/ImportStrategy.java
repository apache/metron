package org.apache.metron.dataloads.nonbulk.flatfile.importer;

import java.util.Optional;

public enum ImportStrategy {
  LOCAL(LocalImporter.INSTANCE),
  MR(MapReduceImporter.INSTANCE)
  ;
  private Importer importer;

  ImportStrategy(Importer importer) {
    this.importer = importer;
  }

  public Importer getImporter() {
    return importer;
  }

  public static Optional<ImportStrategy> getStrategy(String strategyName) {
    if(strategyName == null) {
      return Optional.empty();
    }
    for(ImportStrategy strategy : values()) {
      if(strategy.name().equalsIgnoreCase(strategyName.trim())) {
        return Optional.of(strategy);
      }
    }
    return Optional.empty();
  }
}
