package org.apache.metron.dataloads.nonbulk.flatfile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.enrichment.converter.EnrichmentConverter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public interface Importer {
  void importData(EnumMap<LoadOptions, Optional<Object>> config, ExtractorHandler handler , final Configuration hadoopConfig) throws IOException;
}
