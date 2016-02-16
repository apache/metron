package org.apache.metron.dataloads.extractor.stix;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.stix.types.ObjectTypeHandler;
import org.apache.metron.dataloads.extractor.stix.types.ObjectTypeHandlers;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.mitre.cybox.common_2.*;
import org.mitre.cybox.cybox_2.ObjectType;
import org.mitre.stix.common_1.IndicatorBaseType;
import org.mitre.stix.indicator_2.Indicator;
import org.mitre.stix.stix_1.STIXPackage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cstella on 2/8/16.
 */
public class StixExtractor implements Extractor {
    Map<String, Object> config;
    @Override
    public Iterable<ThreatIntelResults> extract(String line) throws IOException {
        STIXPackage stixPackage = STIXPackage.fromXMLString(line);
        List<ThreatIntelResults> ret = new ArrayList<>();
        if (stixPackage.getIndicators() != null) {
            if (stixPackage.getIndicators().getIndicators() != null) {
                List<IndicatorBaseType> indicators = stixPackage.getIndicators().getIndicators();
                int indicatorCount = indicators.size();
                for (int i = 0; i < indicatorCount; i++) {
                    Indicator indicator = (Indicator) indicators.get(i);
                    if (indicator.getObservable() != null) {
                        ObjectType obj = indicator.getObservable().getObject();
                        ObjectPropertiesType props = obj.getProperties();
                        ObjectTypeHandler handler = ObjectTypeHandlers.getHandlerByInstance(props);
                        if(handler != null) {
                            Iterables.addAll(ret, handler.extract(props, config));
                        }
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        this.config = config;
    }

    public static Iterable<String> split(StringObjectPropertyType value) {
        final ConditionTypeEnum condition = value.getCondition();
        final ConditionApplicationEnum applyCondition = value.getApplyCondition();
        List<String> tokens = new ArrayList<>();
        if(condition == ConditionTypeEnum.EQUALS && applyCondition == ConditionApplicationEnum.ANY) {
            String delim = value.getDelimiter();
            String line = value.getValue().toString();
            if (delim != null) {
                for (String token : Splitter.on(delim).split(line)) {
                    tokens.add(token);
                }
            } else {
                tokens.add(line);
            }
        }
        return tokens;
    }
    public static void main(String[] args) throws IOException {

        File file = new File("/tmp/sample.xml");

        /*if (args.length > 0) {
            file = new File(args[0]);
        } else {
            try {
                URL url = XML2Object.class.getClass().getResource(
                        "/org/mitre/stix/examples/sample.xml");
                file = new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }*/

        String line = FileUtils.readFileToString(file);
        StixExtractor extractor = new StixExtractor();
        for(ThreatIntelResults results : extractor.extract(line)) {
            System.out.println(results);
        }

    }
}
