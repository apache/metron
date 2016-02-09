package org.apache.metron.dataloads.extractor.stix.types;

import com.google.common.base.Splitter;
import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.mitre.cybox.common_2.ConditionApplicationEnum;
import org.mitre.cybox.common_2.ConditionTypeEnum;
import org.mitre.cybox.common_2.StringObjectPropertyType;
import org.mitre.cybox.objects.Address;
import org.mitre.cybox.objects.CategoryTypeEnum;

import java.io.IOException;
import java.util.*;

/**
 * Created by cstella on 2/9/16.
 */
public class AddressHandler extends AbstractObjectTypeHandler<Address> {
    public static final String SPECIFIC_CATEGORY_CONFIG = "stix_address_categories";
    public static final EnumSet<CategoryTypeEnum> SUPPORTED_CATEGORIES = EnumSet.of(CategoryTypeEnum.E_MAIL
                                                                                   ,CategoryTypeEnum.IPV_4_ADDR
                                                                                   ,CategoryTypeEnum.IPV_6_ADDR
                                                                                   ,CategoryTypeEnum.MAC
                                                               ) ;
    public AddressHandler() {
        super(Address.class);
    }

    @Override
    public Iterable<ThreatIntelResults> extract(final Address type, Map<String, Object> config) throws IOException {
        List<ThreatIntelResults> ret = new ArrayList<>();
        final CategoryTypeEnum category= type.getCategory();
        if(!SUPPORTED_CATEGORIES.contains(category)) {
           return ret;
        }
        if(config != null && config.containsKey(SPECIFIC_CATEGORY_CONFIG)) {
            List<CategoryTypeEnum> categories = new ArrayList<>();
            for(String c : Splitter.on(",").split(config.get(SPECIFIC_CATEGORY_CONFIG).toString())) {
                categories.add(CategoryTypeEnum.valueOf(c));
            }
            EnumSet<CategoryTypeEnum> specificCategories = EnumSet.copyOf(categories);
            if(!specificCategories.contains(category)) {
                return ret;
            }

        }
        StringObjectPropertyType value = type.getAddressValue();
        for(String token : StixExtractor.split(value)) {
            ThreatIntelResults results = new ThreatIntelResults(new ThreatIntelKey(token),
                                                                    new HashMap<String, String>() {{
                                                                        put("source-type", "STIX");
                                                                        put("indicator-type", "Address");
                                                                        put("source", type.toXMLString());
                                                                    }}
                                                                   );
                ret.add(results);
        }
        return ret;
    }
}
