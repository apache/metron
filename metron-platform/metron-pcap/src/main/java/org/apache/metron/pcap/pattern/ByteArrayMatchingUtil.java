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
package org.apache.metron.pcap.pattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.byteseek.compiler.CompileException;
import net.byteseek.compiler.matcher.MatcherCompilerUtils;
import net.byteseek.compiler.matcher.SequenceMatcherCompiler;
import net.byteseek.matcher.sequence.SequenceMatcher;
import net.byteseek.searcher.Searcher;
import net.byteseek.searcher.sequence.horspool.BoyerMooreHorspoolSearcher;
import net.byteseek.searcher.sequence.horspool.HorspoolFinalFlagSearcher;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public enum ByteArrayMatchingUtil {
  INSTANCE;
  private LoadingCache<String, Searcher<SequenceMatcher>> sequenceMatchers = CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .build(
                  new CacheLoader<String, Searcher<SequenceMatcher>>() {
                    public Searcher<SequenceMatcher> load(String pattern) throws Exception {
                      return new HorspoolFinalFlagSearcher(compile(pattern));
                    }
                  });
  private SequenceMatcherCompiler compiler = new SequenceMatcherCompiler();

  private SequenceMatcher compile(String pattern) throws CompileException {

    return compiler.compile(pattern);
  }

  public boolean match(String pattern, byte[] data) throws ExecutionException {
    if(pattern == null) {
      return false;
    }
    Searcher<SequenceMatcher> searcher = sequenceMatchers.get(pattern);
    if(data == null) {
      return false;
    }
    else {
      return !searcher.searchForwards(data).isEmpty();
    }
  }
}
