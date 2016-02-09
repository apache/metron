package org.apache.metron.dataloads.extractor.stix.types;

import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.mitre.cybox.common_2.StringObjectPropertyType;
import org.mitre.cybox.objects.DomainName;
import org.mitre.cybox.objects.DomainNameTypeEnum;

import java.io.IOException;
import java.util.*;

/**
 * Created by cstella on 2/9/16.
 */
public class DomainHandler extends AbstractObjectTypeHandler<DomainName> {
    EnumSet<DomainNameTypeEnum> SUPPORTED_TYPES = EnumSet.of(DomainNameTypeEnum.FQDN);
    public DomainHandler() {
        super(DomainName.class);
    }

    @Override
    public Iterable<ThreatIntelResults> extract(final DomainName type, Map<String, Object> config) throws IOException {
        List<ThreatIntelResults> ret = new ArrayList<>();
        final DomainNameTypeEnum domainType = type.getType();
        if(SUPPORTED_TYPES.contains(domainType)) {
            StringObjectPropertyType value = type.getValue();
            for (String token : StixExtractor.split(value)) {
                ThreatIntelResults results = new ThreatIntelResults(new ThreatIntelKey(token),
                        new HashMap<String, String>() {{
                            put("source-type", "STIX");
                            put("indicator-type", "DomainName");
                            put("source", type.toXMLString());
                        }}
                );
                ret.add(results);
            }
        }
        return ret;
    }
}
