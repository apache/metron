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
import { merge } from './context-menu.util';

describe('context-menu.util', () => {

  it('merge function should be able to merge two objects', () => {
    expect(merge( { first: 'aaa' }, { second: 'bbb' } )).toEqual({ first: 'aaa', second: 'bbb' });
  })

  it('merge should be able to merge many objects', () => {
    const objects = [
      { first: 'aaa' },
      { second: 'bbb' },
      { third: 'ccc' },
      { fourth: 'ddd' },
      { fiveth: 'eee' },
      { sixth: 'fff' },
      { seventh: 'ggg' },
    ];

    expect(merge.apply(null, objects)).toEqual(
      objects.reduce((result, next) => Object.assign(result, next), {})
    );
  })

});
