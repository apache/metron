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

package org.apache.metron.stellar.dsl.functions;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.InternetDomainName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class NetworkFunctions {
  @Stellar(name="IN_SUBNET"
          ,description = "Returns true if an IP is within a subnet range."
          ,params = {
                     "ip - The IP address in string form"
                    ,"cidr+ - One or more IP ranges specified in CIDR notation (for example 192.168.0.0/24)"
                    }
          ,returns = "True if the IP address is within at least one of the network ranges and false if otherwise"
          )
  public static class InSubnet extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        throw new IllegalStateException("IN_SUBNET expects at least two args: [ip, cidr1, cidr2, ...]"
                + " where cidr is the subnet mask in cidr form"
        );
      }
      String ip = (String) list.get(0);
      if(ip == null) {
        return false;
      }
      boolean inSubnet = false;
      for(int i = 1;i < list.size() && !inSubnet;++i) {
        String cidr = (String) list.get(i);
        if(cidr == null) {
          continue;
        }
        inSubnet |= new SubnetUtils(cidr).getInfo().isInRange(ip);
      }

      return inSubnet;
    }
  }

  @Stellar(name="REMOVE_SUBDOMAINS"
          ,namespace = "DOMAIN"
          ,description = "Removes the subdomains from a domain."
          , params = {
                      "domain - Fully qualified domain name"
                     }
          , returns = "The domain without the subdomains.  " +
                      "(for example, DOMAIN_REMOVE_SUBDOMAINS('mail.yahoo.com') yields 'yahoo.com')"
          )
  public static class RemoveSubdomains extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> objects) {
      if(objects.isEmpty()) {
        return null;
      }
      Object dnObj = objects.get(0);
      InternetDomainName idn = toDomainName(dnObj);
      if(idn != null) {
        String dn = dnObj.toString();
        String tld = extractTld(idn, dn);
        if(!StringUtils.isEmpty(dn)) {
          String suffix = safeSubstring(dn, 0, dn.length() - tld.length());
          String hostnameWithoutTLD = safeSubstring(suffix, 0, suffix.length() - 1);
          if(hostnameWithoutTLD == null) {
            return dn;
          }
          String hostnameWithoutSubsAndTLD = Iterables.getLast(Splitter.on(".").split(hostnameWithoutTLD), null);
          if(hostnameWithoutSubsAndTLD == null) {
            return null;
          }
          return hostnameWithoutSubsAndTLD + "." + tld;
        }
      }
      return null;
    }
  }

  @Stellar(name="REMOVE_TLD"
          ,namespace = "DOMAIN"
          ,description = "Removes the top level domain (TLD) suffix from a domain."
          , params = {
                      "domain - Fully qualified domain name"
                     }
          , returns = "The domain without the TLD.  " +
                      "(for example, DOMAIN_REMOVE_TLD('mail.yahoo.co.uk') yields 'mail.yahoo')"
          )
  public static class RemoveTLD extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> objects) {
      Object dnObj = objects.get(0);
      InternetDomainName idn = toDomainName(dnObj);
      if(idn != null) {
        String dn = dnObj.toString();
        String tld = extractTld(idn, dn);
        String suffix = safeSubstring(dn, 0, dn.length() - tld.length());
        if(StringUtils.isEmpty(suffix)) {
          return suffix;
        }
        else {
          return suffix.substring(0, suffix.length() - 1);
        }
      }
      return null;
    }
  }

  @Stellar(name="TO_TLD"
          ,namespace = "DOMAIN"
          ,description = "Extracts the top level domain from a domain"
          , params = {
                      "domain - Fully qualified domain name"
                     }
          , returns = "The TLD of the domain.  " +
                      "(for example, DOMAIN_TO_TLD('mail.yahoo.co.uk') yields 'co.uk')"
          )
  public static class ExtractTLD extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> objects) {
      Object dnObj = objects.get(0);
      InternetDomainName idn = toDomainName(dnObj);
      return extractTld(idn, dnObj + "");
    }
  }

  @Stellar(name="TO_PORT"
          ,namespace="URL"
          ,description = "Extract the port from a URL.  " +
                          "If the port is not explicitly stated in the URL, then an implicit port is inferred based on the protocol."
          , params = {
                      "url - URL in string form"
                     }
          , returns = "The port used in the URL as an integer (for example, URL_TO_PORT('http://www.yahoo.com/foo') would yield 80)"
          )
  public static class URLToPort extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> objects) {
      URL url =  toUrl(objects.get(0));
      if(url == null) {
        return null;
      }
      int port = url.getPort();
      return port >= 0?port:url.getDefaultPort();
    }
  }

  @Stellar(name="TO_PATH"
          ,namespace="URL"
          ,description = "Extract the path from a URL."
          , params = {
                      "url - URL in String form"
                     }
          , returns = "The path from the URL as a String.  e.g. URL_TO_PATH('http://www.yahoo.com/foo') would yield 'foo'")
  public static class URLToPath extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> objects) {
      URL url =  toUrl(objects.get(0));
      return url == null?null:url.getPath();
    }
  }

  @Stellar(name="TO_HOST"
          ,namespace="URL"
          ,description = "Extract the hostname from a URL."
          , params = {
                      "url - URL in String form"
                     }
          , returns = "The hostname from the URL as a String.  e.g. URL_TO_HOST('http://www.yahoo.com/foo') would yield 'www.yahoo.com'"
          )
  public static class URLToHost extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> objects) {
      URL url =  toUrl(objects.get(0));
      return url == null?null:url.getHost();
    }
  }

  @Stellar(name="TO_PROTOCOL"
          ,namespace="URL"
          ,description = "Extract the protocol from a URL."
          , params = {
                      "url - URL in String form"
                     }
          , returns = "The protocol from the URL as a String. e.g. URL_TO_PROTOCOL('http://www.yahoo.com/foo') would yield 'http'")
  public static class URLToProtocol extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> objects) {
      URL url =  toUrl(objects.get(0));
      return url == null?null:url.getProtocol();
    }
  }

  /**
   * Extract the TLD.  If the domain is a normal domain, then we can handle the TLD via the InternetDomainName object.
   * If it is not, then we default to returning the last segment after the final '.'
   * @param idn
   * @param dn
   * @return The TLD of the domain
   */
  private static String extractTld(InternetDomainName idn, String dn) {

    if(idn != null && idn.hasPublicSuffix()) {
      return idn.publicSuffix().toString();
    }
    else if(dn != null) {
      StringBuffer tld = new StringBuffer("");
      for(int idx = dn.length() -1;idx >= 0;idx--) {
        char c = dn.charAt(idx);
        if(c == '.') {
          break;
        }
        else {
          tld.append(dn.charAt(idx));
        }
      }
      return tld.reverse().toString();
    }
    else {
      return null;
    }
  }

  private static String safeSubstring(String val, int start, int end) {
    if(!StringUtils.isEmpty(val)) {
      return val.substring(start, end);
    }
    return null;
  }

  private static InternetDomainName toDomainName(Object dnObj) {
    if(dnObj != null) {
      if(dnObj instanceof String) {
        String dn = dnObj.toString();
        try {
          return InternetDomainName.from(dn);
        }
        catch(IllegalArgumentException iae) {
          return null;
        }
      }
      else {
        throw new IllegalArgumentException(dnObj + " is not a string and therefore also not a domain.");
      }
    }
    return null;
  }

  private static URL toUrl(Object urlObj) {
    if(urlObj == null) {
      return null;
    }
    if(urlObj instanceof String) {
      String url = urlObj.toString();
      try {
        return new URL(url);
      } catch (MalformedURLException e) {
        return null;
      }
    }
    else {
      throw new IllegalArgumentException(urlObj + " is not a string and therefore also not a URL.");
    }
  }
}
