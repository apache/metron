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
import {AutocompleteTransformationsStatement} from './autocomplete-transformations-statement';
import {AutocompleteOption} from '../../model/autocomplete-option';

describe('AutocompleteTransformationsStatement', () => {
  let functionOption: AutocompleteOption = new AutocompleteOption();
  functionOption.name = 'function';
  functionOption.isFunction = true;
  let fieldOption: AutocompleteOption = new AutocompleteOption();
  fieldOption.name = 'field';
  fieldOption.isFunction = false;

  it('should create an instance', () => {
    expect(new AutocompleteTransformationsStatement()).toBeTruthy();
  });

  it('should return function', () => {
    let autocompleteTransformationsStatement = new AutocompleteTransformationsStatement();

    expect(autocompleteTransformationsStatement.getFunctionSignature('(', functionOption, '')).toEqual('function()');
    expect(autocompleteTransformationsStatement.getFunctionSignature('', functionOption, '')).toEqual('function()');
    expect(autocompleteTransformationsStatement.getFunctionSignature('', fieldOption, '')).toEqual('field');
  });

  it('should return Index Of Next Token', () => {
    let autocompleteTransformationsStatement = new AutocompleteTransformationsStatement();

    expect(autocompleteTransformationsStatement.getIndexOfNextToken('test(a,b)', 3)).toEqual(5);
    expect(autocompleteTransformationsStatement.getIndexOfNextToken('test(a,b)', 4)).toEqual(5);
    expect(autocompleteTransformationsStatement.getIndexOfNextToken('test(a,b)', 6)).toEqual(7);
    expect(autocompleteTransformationsStatement.getIndexOfNextToken('test(a,b)', 8)).toEqual(9);
  });
});
