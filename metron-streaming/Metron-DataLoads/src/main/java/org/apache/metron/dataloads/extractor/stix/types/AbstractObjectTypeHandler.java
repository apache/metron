package org.apache.metron.dataloads.extractor.stix.types;

import org.mitre.cybox.common_2.ObjectPropertiesType;
import org.mitre.cybox.common_2.StringObjectPropertyType;

/**
 * Created by cstella on 2/9/16.
 */
public abstract class AbstractObjectTypeHandler<T extends ObjectPropertiesType> implements ObjectTypeHandler<T> {
    protected Class<T> objectPropertiesType;
    public AbstractObjectTypeHandler(Class<T> clazz) {
        objectPropertiesType = clazz;
    }
    @Override
    public Class<T> getTypeClass() {
        return objectPropertiesType;
    }


}
