/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.dataloads.extractor;

import org.apache.metron.dataloads.extractor.csv.CSVExtractor;
import org.apache.metron.dataloads.extractor.stix.StixExtractor;

import java.util.Map;

public enum Extractors implements ExtractorCreator {
    CSV(new ExtractorCreator() {

        @Override
        public Extractor create() {
            return new CSVExtractor();
        }
    })
    ,STIX(new ExtractorCreator() {
        @Override
        public Extractor create() {
            return new StixExtractor();
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
