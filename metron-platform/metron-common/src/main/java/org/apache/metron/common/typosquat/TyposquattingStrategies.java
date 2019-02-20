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
package org.apache.metron.common.typosquat;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a set of strategies for generating likely typos from domains.
 */
public enum TyposquattingStrategies implements TyposquattingStrategy {
  ADDITION(new AdditionStrategy()),
  BITSQUATTING(new BitsquattingStrategy()),
  HOMOGLYPH(new HomoglyphStrategy()),
  HYPHENATION(new HyphenationStrategy()),
  INSERTION(new InsertionStrategy()),
  OMISSION(new OmissionStrategy()),
  REPETITION(new RepetitionStrategy()),
  REPLACEMENT(new ReplacementStrategy()),
  SUBDOMAIN(new SubdomainStrategy()),
  TRANSPOSITION(new TranspositionStrategy()),
  VOWELSWAP(new VowelSwapStrategy()),
  ;
  TyposquattingStrategy strategy;
  TyposquattingStrategies(TyposquattingStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public Set<String> generateCandidates(String originalString) {
    Set<String> candidates = strategy.generateCandidates(originalString);
    candidates.remove(originalString);
    return candidates;
  }

  /**
   * Generates all typosquatting candidates.
   *
   * @param originalString The original string to generate candidates for
   * @return A set of candidates for typosquatting
   */
  public static Set<String> generateAllCandidates(String originalString) {
    Set<String> ret = new HashSet<>();
    for(TyposquattingStrategy s : values() ) {
      ret.addAll(s.generateCandidates(originalString));
    }
    return ret;
  }

  /**
   * Retrieves a strategy by name.
   *
   * @param name The name of the strategy
   * @return The associated {@link TyposquattingStrategy}
   */
  public static TyposquattingStrategies byName(String name) {
    for(TyposquattingStrategies s : values()) {
      if(s.strategy.name().equals(name)) {
        return s;
      }
    }
    return null;
  }

  @Stellar(namespace="DOMAIN"
          ,name="TYPOSQUAT"
          ,description="Generate potential typosquatted domain from a passed domain.  Strategies largely match https://github.com/elceef/dnstwist"
          ,params = {
              "domain - Domain (without subdomains or TLD) to generate typosquatted domains for."
                    }
          ,returns = "A set of potential typosquatted domains."
          )
  public static class Generate implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() == 0) {
        return null;
      }
      Object dnObj = args.get(0);
      if(dnObj == null) {
        return null;
      }
      if(!(dnObj instanceof String)) {
        throw new IllegalStateException("Expected a domain without subdomains or a TLD, but got " + dnObj);
      }
      return TyposquattingStrategies.generateAllCandidates((String)dnObj);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }
}
