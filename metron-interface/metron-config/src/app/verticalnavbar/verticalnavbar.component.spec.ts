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
import {TestBed} from '@angular/core/testing';
import {Router} from '@angular/router';
import {VerticalNavbarComponent} from './verticalnavbar.component';

class MockRouter {
  url = '';
}

describe('VerticalNavbarComponent', () => {
  let router: Router;
  let verticalNavbarComponent: VerticalNavbarComponent;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        VerticalNavbarComponent,
        {provide: Router, useClass: MockRouter}
      ]
    });
    verticalNavbarComponent = TestBed.get(VerticalNavbarComponent);
  });

  it('can instantiate VerticalNavbarComponent', () => {
      expect(verticalNavbarComponent instanceof VerticalNavbarComponent).toBe(true);
  });

});
