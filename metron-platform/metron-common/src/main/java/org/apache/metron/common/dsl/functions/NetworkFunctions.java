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

package org.apache.metron.common.dsl.functions;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.InternetDomainName;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

public class NetworkFunctions {
  @Stellar(name="IN_SUBNET"
          ,description = "Returns if an IP is within a subnet range."
          ,params = {
                     "ip - the IP address in String form"
                    ,"cidr+ - one or more IP ranges specified in CIDR notation (e.g. 192.168.0.0/24)"
                    }
          ,returns = "True if the IP address is within at least one of the network ranges and false otherwise"
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
        String cidr = (String) list.get(1);
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
          ,description = "Remove subdomains from a domain."
          , params = {
                      "domain - fully qualified domain name"
                     }
          , returns = "The domain without the subdomains.  " +
                      "e.g. DOMAIN_REMOVE_SUBDOMAINS('mail.yahoo.com') yields 'yahoo.com'"
          )
  public static class RemoveSubdomains extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> objects) {
      Object dnObj = objects.get(0);
      InternetDomainName idn = toDomainName(dnObj);
      if(idn != null) {
        String dn = dnObj.toString();
        String tld = idn.publicSuffix().toString();
        String suffix = Iterables.getFirst(Splitter.on(tld).split(dn), null);
        if(suffix != null)
        {
          String hostnameWithoutTLD = suffix.substring(0, suffix.length() - 1);
          String hostnameWithoutSubsAndTLD = Iterables.getLast(Splitter.on(".").split(hostnameWithoutTLD), null);
          if(hostnameWithoutSubsAndTLD == null) {
            return null;
          }
          return hostnameWithoutSubsAndTLD + "." + tld;
        }
        else {
          return null;
        }
      }
      return null;
    }
  }

  @Stellar(name="REMOVE_TLD"
          ,namespace = "DOMAIN"
          ,description = "Remove top level domain suffix from a domain."
          , params = {
                      "domain - fully qualified domain name"
                     }
          , returns = "The domain without the TLD.  " +
                      "e.g. DOMAIN_REMOVE_TLD('mail.yahoo.co.uk') yields 'mail.yahoo'"
          )
  public static class RemoveTLD extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> objects) {
      Object dnObj = objects.get(0);
      InternetDomainName idn = toDomainName(dnObj);
      if(idn != null) {
        String dn = dnObj.toString();
        String tld = idn.publicSuffix().toString();
        String suffix = Iterables.getFirst(Splitter.on(tld).split(dn), null);
        if(suffix != null)
        {
          return suffix.substring(0, suffix.length() - 1);
        }
        else {
          return null;
        }
      }
      return null;
    }
  }

  @Stellar(name="TO_TLD"
          ,namespace = "DOMAIN"
          ,description = "Extract the top level domain from a domain"
          , params = {
                      "domain - fully qualified domain name"
                     }
          , returns = "The TLD of the domain.  " +
                      "e.g. DOMAIN_TO_TLD('mail.yahoo.co.uk') yields 'co.uk'"
          )
  public static class ExtractTLD extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> objects) {
      Object dnObj = objects.get(0);
      InternetDomainName idn = toDomainName(dnObj);
      if(idn != null) {
        return idn.publicSuffix().toString();
      }
      return null;
    }
  }

  @Stellar(name="TO_PORT"
          ,namespace="URL"
          ,description = "Extract the port from a URL.  " +
                          "If the port is not explicitly stated in the URL, then an implicit port is inferred based on the protocol."
          , params = {
                      "url - URL in String form"
                     }
          , returns = "The port used in the URL as an Integer.  e.g. URL_TO_PORT('http://www.yahoo.com/foo') would yield 80"
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

  private static InternetDomainName toDomainName(Object dnObj) {
    if(dnObj != null) {
      String dn = dnObj.toString();
      return InternetDomainName.from(dn);
    }
    return null;
  }

  private static URL toUrl(Object urlObj) {
    if(urlObj == null) {
      return null;
    }
    try {
      return new URL(urlObj.toString());
    } catch (MalformedURLException e) {
      return null;
    }
  }
}
