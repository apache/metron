package org.apache.metron.dataloads.extractor;

import org.apache.metron.dataloads.extractor.csv.CSVExtractor;

import java.util.Map;

/**
 * Created by cstella on 2/2/16.
 */
public enum Extractors implements ExtractorCreator {
    CSV(new ExtractorCreator() {

        @Override
        public Extractor create() {
            return new CSVExtractor();
        }
    })
    ;
    ExtractorCreator _creator;
    Extractors(ExtractorCreator creator) {
        this._creator = creator;
    }
    @Override
    public Extractor create() {
        return _creator.create();
    }
    public static Extractor create(String extractorName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        try {
            ExtractorCreator ec = Extractors.valueOf(extractorName);
            return ec.create();
        }
        catch(IllegalArgumentException iae) {
            Extractor ex = (Extractor) Class.forName(extractorName).newInstance();
            return ex;
        }
    }
}
