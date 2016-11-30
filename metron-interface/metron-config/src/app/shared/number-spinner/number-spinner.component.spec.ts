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
/* tslint:disable:no-unused-variable */

import {NumberSpinnerComponent} from './number-spinner.component';

describe('NumberSpinnerComponent', () => {

  it('should create an instance', () => {
    expect(new NumberSpinnerComponent()).toBeTruthy();
  });

  it('spec for all inherited functions', () => {
    let numberSpinnerComponent = new NumberSpinnerComponent();

    numberSpinnerComponent.writeValue(10);
    expect(numberSpinnerComponent.innerValue).toEqual(10);
    expect(numberSpinnerComponent.value).toEqual(10);

    numberSpinnerComponent.registerOnChange((_: any) => {});
    numberSpinnerComponent.value = 11;
    expect(numberSpinnerComponent.innerValue).toEqual(11);
    expect(numberSpinnerComponent.value).toEqual(11);

    numberSpinnerComponent.value = 'abc';
    expect(numberSpinnerComponent.innerValue).toEqual(11);
    expect(numberSpinnerComponent.value).toEqual(11);

  });

});
