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
import { DynamicMenuItem } from './dynamic-item.model';

describe('dynamic-item.model', () => {

  it('should return error if url pattern is missing', () => {
    expect(DynamicMenuItem.isConfigValid({ label: 'test' })).toBeFalsy();
  });

  it('should return error if label is missing', () => {
    expect(DynamicMenuItem.isConfigValid({ urlPattern: '/test' })).toBeFalsy();
  });

  it('should return error if url pattern is empty', () => {
    expect(DynamicMenuItem.isConfigValid({ label: '', urlPattern: '/test' })).toBeFalsy();
  });

  it('should return error if label is empty', () => {
    expect(DynamicMenuItem.isConfigValid({ label: 'test', urlPattern: '' })).toBeFalsy();
  });

  it('should instatiate if all good', () => {
    expect(DynamicMenuItem.isConfigValid({ label: 'test', urlPattern: '/test' })).toBeTruthy();
  });

});
