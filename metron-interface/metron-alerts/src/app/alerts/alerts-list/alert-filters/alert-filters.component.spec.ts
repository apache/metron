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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CollapseComponent } from '../../../shared/collapse/collapse.component';
import { AlertFiltersComponent } from './alert-filters.component';
import { CenterEllipsesPipe } from '../../../shared/pipes/center-ellipses.pipe';
import { ColumnNameTranslatePipe } from '../../../shared/pipes/column-name-translate.pipe';

describe('AlertFiltersComponent', () => {
  let component: AlertFiltersComponent;
  let fixture: ComponentFixture<AlertFiltersComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ 
        CollapseComponent,
        CenterEllipsesPipe,
        ColumnNameTranslatePipe,
        AlertFiltersComponent,
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AlertFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
