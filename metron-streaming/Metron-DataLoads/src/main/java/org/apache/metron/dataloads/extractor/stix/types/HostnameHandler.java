package org.apache.metron.dataloads.extractor.stix.types;

import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.mitre.cybox.common_2.StringObjectPropertyType;
import org.mitre.cybox.objects.Hostname;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cstella on 2/9/16.
 */
public class HostnameHandler  extends AbstractObjectTypeHandler<Hostname>{
    public HostnameHandler() {
        super(Hostname.class);
    }

    @Override
    public Iterable<LookupKV> extract(final Hostname type, Map<String, Object> config) throws IOException {
        StringObjectPropertyType value = type.getHostnameValue();
        List<LookupKV> ret = new ArrayList<>();
        for(String token : StixExtractor.split(value)) {
            LookupKV results = new LookupKV(new ThreatIntelKey(token)
                                           , new ThreatIntelValue(new HashMap<String, String>() {{
                                                                        put("source-type", "STIX");
                                                                        put("indicator-type", "Hostname");
                                                                        put("source", type.toXMLString());
                                                                    }}
                                                                 )
                                           );
                ret.add(results);
        }
        return ret;
    }
}
