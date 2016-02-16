package org.apache.metron.dataloads.extractor.stix.types;

import org.mitre.cybox.common_2.ObjectPropertiesType;

/**
 * Created by cstella on 2/9/16.
 */
public enum ObjectTypeHandlers {
      ADDRESS(new AddressHandler())
    ,HOSTNAME(new HostnameHandler())
    ,DOMAINNAME(new DomainHandler())
    ,;
   ObjectTypeHandler _handler;
   ObjectTypeHandlers(ObjectTypeHandler handler) {
      _handler = handler;
   }
   ObjectTypeHandler getHandler() {
      return _handler;
   }
   public static ObjectTypeHandler getHandlerByInstance(ObjectPropertiesType inst) {
      for(ObjectTypeHandlers h : values()) {
         if(inst.getClass().equals(h.getHandler().getTypeClass())) {
            return h.getHandler();
         }
      }
      return null;
   }
}
