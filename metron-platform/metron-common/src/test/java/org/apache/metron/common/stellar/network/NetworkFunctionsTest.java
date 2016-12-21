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
package org.apache.metron.common.stellar.network;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.HashMap;

import static org.apache.metron.common.utils.StellarProcessorUtils.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NetworkFunctionsTest {

  @Test
  public void inSubnetTest_positive() {
    assertTrue((Boolean) run("IN_SUBNET(ip, cidr)"
                            , ImmutableMap.of("ip", "192.168.0.1", "cidr", "192.168.0.0/24")
            ));
  }

  @Test
  public void inSubnetTest_negative() {
    assertFalse((Boolean) run("IN_SUBNET(ip, cidr)"
                             , ImmutableMap.of("ip", "192.168.1.1", "cidr", "192.168.0.0/24")
               ));
  }

  @Test
  public void inSubnetTest_multiple() {
    assertTrue((Boolean) run("IN_SUBNET(ip, cidr1, cidr2)"
                            , ImmutableMap.of("ip", "192.168.1.1"
                                             , "cidr1", "192.168.0.0/24"
                                             , "cidr2", "192.168.1.0/24")
              ));
  }

  private static void runSimple(String function, String argument, Object expected) {
    assertEquals(expected, run(function + "(var)"
                                      , ImmutableMap.of("var", argument)
                  ));
  }

  @Test
  public void removeSubdomainsTest() {
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "www.google.co.uk", "google.co.uk");
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "www.google.com", "google.com");
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "com", "com");
  }

  @Test
  public void removeSubdomainsTest_tld_square() {
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "com.com", "com.com");
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "net.net", "net.net");
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "co.uk.co.uk", "uk.co.uk");
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "www.subdomain.com.com", "com.com");
  }

  @Test
  public void removeSubdomainsTest_unknowntld() {
    runSimple("DOMAIN_REMOVE_SUBDOMAINS", "www.subdomain.google.gmail", "google.gmail");
  }

  @Test
  public void toTldTest() {
    runSimple("DOMAIN_TO_TLD", "www.google.co.uk", "co.uk");
    runSimple("DOMAIN_TO_TLD", "www.google.com", "com");
    runSimple("DOMAIN_TO_TLD", "com", "com");
  }

  @Test
  public void toTldTest_tld_square() {
    runSimple("DOMAIN_TO_TLD", "com.com", "com");
    runSimple("DOMAIN_TO_TLD", "net.net", "net");
    runSimple("DOMAIN_TO_TLD", "co.uk.co.uk", "co.uk");
    runSimple("DOMAIN_TO_TLD", "www.subdomain.com.com", "com");
  }

  @Test
  public void toTldTest_unknowntld() {
    runSimple("DOMAIN_TO_TLD", "www.subdomain.google.gmail", "gmail");
  }

  @Test
  public void removeTldTest() {
    runSimple("DOMAIN_REMOVE_TLD", "www.google.co.uk", "www.google");
    runSimple("DOMAIN_REMOVE_TLD", "www.google.com", "www.google");
    runSimple("DOMAIN_REMOVE_TLD", "com", "");
  }

  @Test
  public void removeTldTest_tld_square() {
    runSimple("DOMAIN_REMOVE_TLD", "com.com", "com");
    runSimple("DOMAIN_REMOVE_TLD", "net.net", "net");
    runSimple("DOMAIN_REMOVE_TLD", "co.uk.co.uk", "co.uk");
    runSimple("DOMAIN_REMOVE_TLD", "www.subdomain.com.com", "www.subdomain.com");
  }

  @Test
  public void removeTldTest_unknowntld() {
    runSimple("DOMAIN_REMOVE_TLD", "www.subdomain.google.gmail", "www.subdomain.google");
  }

  @Test
  public void urlToPortTest() {
    runSimple("URL_TO_PORT", "http://www.google.com/foo/bar", 80);
    runSimple("URL_TO_PORT", "https://www.google.com/foo/bar", 443);
    runSimple("URL_TO_PORT", "http://www.google.com:7979/foo/bar", 7979);
  }


  @Test
  public void urlToPortTest_unknowntld() {
    runSimple("URL_TO_PORT", "http://www.google.gmail/foo/bar", 80);
  }

  @Test
  public void urlToHostTest() {
    runSimple("URL_TO_HOST", "http://www.google.com/foo/bar", "www.google.com");
    runSimple("URL_TO_HOST", "https://www.google.com/foo/bar", "www.google.com");
    runSimple("URL_TO_HOST", "http://www.google.com:7979/foo/bar", "www.google.com");
  }


  @Test
  public void urlToHostTest_unknowntld() {
    runSimple("URL_TO_HOST", "http://www.google.gmail/foo/bar", "www.google.gmail");
  }

  @Test
  public void urlToProtocolTest() {
    runSimple("URL_TO_PROTOCOL", "http://www.google.com/foo/bar", "http");
    runSimple("URL_TO_PROTOCOL", "https://www.google.com/foo/bar", "https");
  }


  @Test
  public void urlToProtocolTest_unknowntld() {
    runSimple("URL_TO_PROTOCOL", "http://www.google.gmail/foo/bar", "http");
  }

  @Test
  public void urlToProtocolTest_unknownprotocol() {
    runSimple("URL_TO_PROTOCOL", "casey://www.google.gmail/foo/bar", "casey");
  }

}
