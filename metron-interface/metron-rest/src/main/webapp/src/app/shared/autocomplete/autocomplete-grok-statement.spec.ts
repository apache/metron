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
import {AutocompleteGrokStatement} from './autocomplete-grok-statement';
import {AutocompleteOption} from '../../model/autocomplete-option';

describe('AutocompleteGrokStatement', () => {
  let option: AutocompleteOption = new AutocompleteOption();
  option.name = 'test';
  option.isFunction = true;

  it('should create an instance', () => {
    expect(new AutocompleteGrokStatement()).toBeTruthy();
  });

  it('should return function', () => {
    let autocompleteGrokStatement = new AutocompleteGrokStatement();
    expect(autocompleteGrokStatement.getFunctionSignature('%{', option, '')).toBeTruthy('%{test:}');
    expect(autocompleteGrokStatement.getFunctionSignature('%{', option, ':')).toBeTruthy('%{test:');
    expect(autocompleteGrokStatement.getFunctionSignature('%{', option, ':}')).toBeTruthy('%{test:');
    expect(autocompleteGrokStatement.getFunctionSignature('', option, ':}')).toBeTruthy('%{test:');
    expect(autocompleteGrokStatement.getFunctionSignature('{', option, ':}')).toBeTruthy('%{test:');
  });

  it('should return Index Of Next Token', () => {
    let autocompleteGrokStatement = new AutocompleteGrokStatement();

    expect(autocompleteGrokStatement.getIndexOfNextToken('%{test: }', 3)).toEqual(7);
    expect(autocompleteGrokStatement.getIndexOfNextToken('%{test: }', 5)).toEqual(7);
    expect(autocompleteGrokStatement.getIndexOfNextToken('%{test: }', 7)).toEqual(9);
    expect(autocompleteGrokStatement.getIndexOfNextToken('%{test: }', 8)).toEqual(9);
  });
});
