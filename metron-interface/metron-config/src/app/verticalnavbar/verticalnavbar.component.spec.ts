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
import {async, inject, TestBed} from '@angular/core/testing';
import {Router} from '@angular/router';
import {VerticalNavbarComponent} from './verticalnavbar.component';

class MockRouter {
  url: string = '';
}

describe('VerticalNavbarComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        VerticalNavbarComponent,
        {provide: Router, useClass: MockRouter}
      ]
    }).compileComponents();

  }));

  it('can instantiate VerticalNavbarComponent',
    inject([VerticalNavbarComponent], (verticalNavbarComponent: VerticalNavbarComponent) => {
      expect(verticalNavbarComponent instanceof VerticalNavbarComponent).toBe(true);
  }));

  it('check isActive for a URL VerticalNavbarComponent',
    inject([VerticalNavbarComponent, Router], (component: VerticalNavbarComponent, router: Router) => {

      router.url = '/abc';
      expect(component.isActive(['/def'])).toEqual(false);
      expect(component.isActive(['/abc'])).toEqual(true);
      expect(component.isActive(['/def', '/abc'])).toEqual(true);

  }));

});
