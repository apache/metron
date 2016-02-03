package org.apache.metron.dataloads.extractor;

import java.util.Map;

/**
 * Created by cstella on 2/2/16.
 */
public interface ExtractorCreator {
    Extractor create();
}
