/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.common.shell;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Provides auto-completion for Stellar.
 */
public class DefaultStellarAutoCompleter implements StellarAutoCompleter {

  enum OperationType {
    DOC,
    MAGIC,
    NORMAL
  }

  enum AutoCompleteType implements AutoCompleteTransformation {
    FUNCTION((type, key) -> {
      if(OperationType.DOC == type) {
        return "?" + key;

      } else if(OperationType.NORMAL == type) {
        return key + "(";
      }

      return key;
    }),
    VARIABLE((type, key) -> key),
    TOKEN((type, key) -> key);

    AutoCompleteTransformation transform;

    AutoCompleteType(AutoCompleteTransformation transform) {
      this.transform = transform;
    }

    @Override
    public String transform(OperationType type, String key) {
      return transform.transform(type, key);
    }
  }

  /**
   * Prefix tree index of auto-completes.
   */
  private PatriciaTrie<AutoCompleteType> autocompleteIndex;

  private ReadWriteLock indexLock = new ReentrantReadWriteLock();

  public interface AutoCompleteTransformation {
    String transform(OperationType type, String key);
  }

  public DefaultStellarAutoCompleter() {
    this.autocompleteIndex = initializeIndex();
  }

  @Override
  public Iterable<String> autoComplete(String buffer) {
    Iterable<String> candidates = IterableUtils.emptyIterable();

    final String lastToken = getLastToken(buffer);
    if(StringUtils.isNotEmpty(lastToken)) {

      if (isDoc(lastToken)) {
        candidates = autoCompleteDoc(lastToken.substring(1));

      } else if (isMagic(lastToken)) {
        candidates = autoCompleteMagic(lastToken);

      } else {
        candidates = autoCompleteNormal(lastToken);
      }
    }

    return candidates;
  }

  /**
   * Is a given expression a built-in magic?
   * @param expression The expression.
   */
  private boolean isMagic(String expression) {
    return StringUtils.startsWith(expression, "%");
  }

  /**
   * Is a given expression asking for function documentation?
   * @param expression The expression.
   */
  private boolean isDoc(String expression) {
    return StringUtils.startsWith(expression, "?");
  }

  /**
   * Auto-completes a partial Stellar expression
   * @param buffer The partial buffer that needs auto-completed.
   * @return Viable candidates for auto-completion.
   */
  private Iterable<String> autoCompleteNormal(String buffer) {
    return autoComplete(buffer, OperationType.NORMAL);
  }

  /**
   * Auto-completes a partial doc command.
   * @param buffer The partial buffer that needs auto-completed.
   * @return Viable candidates for auto-completion.
   */
  private Iterable<String> autoCompleteDoc(String buffer) {
    return autoComplete(buffer, OperationType.DOC);
  }

  /**
   * Auto-completes a partial magic commands.
   * @param buffer The partial buffer that needs auto-completed.
   * @return Viable candidates for auto-completion.
   */
  private Iterable<String> autoCompleteMagic(String buffer) {
    return autoComplete(buffer, OperationType.MAGIC);
  }

  /**
   * Returns a list of viable candidates for auto-completion.
   * @param buffer The current buffer.
   * @param opType The type of operation needing auto-completion.
   * @return Viable candidates for auto-completion.
   */
  private Iterable<String> autoComplete(String buffer, final OperationType opType) {
    indexLock.readLock().lock();
    try {
      SortedMap<String, AutoCompleteType> ret = autocompleteIndex.prefixMap(buffer);
      if (ret.isEmpty()) {
        return new ArrayList<>();
      }
      return Iterables.transform(ret.entrySet(), kv -> kv.getValue().transform(opType, kv.getKey()));
    }
    finally {
      indexLock.readLock().unlock();
    }
  }

  /**
   * Adds a candidate for auto-completing function names.
   * @param name The name of the function candidate.
   */
  @Override
  public void addCandidateFunction(String name) {
    add(name, AutoCompleteType.FUNCTION);
  }

  /**
   * Adds a candidate for auto-completing variable names.
   * @param name The name of the function candidate.
   */
  @Override
  public void addCandidateVariable(String name) {
    add(name, AutoCompleteType.VARIABLE);
  }

  /**
   * Add a candidate for auto-completion.
   * @param name The name of the candidate.
   * @param type The type of candidate.
   */
  private void add(String name, AutoCompleteType type) {
    if(StringUtils.isNotBlank(name)) {
      // add the candidate to the auto-complete index
      indexLock.writeLock().lock();
      try {
        this.autocompleteIndex.put(name, type);
      } finally {
        indexLock.writeLock().unlock();
      }
    }
  }

  private PatriciaTrie<AutoCompleteType> initializeIndex() {
    Map<String, AutoCompleteType> index = new HashMap<>();
    index.put("==", AutoCompleteType.TOKEN);
    index.put(">=", AutoCompleteType.TOKEN);
    index.put("<=", AutoCompleteType.TOKEN);

    return new PatriciaTrie<>(index);
  }

  private static String getLastToken(String buffer) {
    String lastToken = Iterables.getLast(Splitter.on(" ").split(buffer), null);
    return lastToken.trim();
  }
}
