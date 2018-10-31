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

import { SavedSearchesComponent } from './saved-searches.component';
import { CollapseComponent } from '../../shared/collapse/collapse.component';
import { CenterEllipsesPipe } from '../../shared/pipes/center-ellipses.pipe';
import { ColumnNameTranslatePipe } from '../../shared/pipes/column-name-translate.pipe';
import { Router } from '@angular/router';
import { SaveSearchService } from '../../service/save-search.service';
import { of } from 'rxjs';
import { DialogService } from 'app/service/dialog.service';


describe('SavedSearchesComponent', () => {
  let component: SavedSearchesComponent;
  let fixture: ComponentFixture<SavedSearchesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: {} },
        { provide: SaveSearchService, useValue: {
          listSavedSearches: jasmine.createSpy('listSavedSearches').and.returnValue(of([])),
          listRecentSearches: jasmine.createSpy('listRecentSearches').and.returnValue(of([])),
        } },
        DialogService
      ],
      declarations: [
        SavedSearchesComponent,
        CollapseComponent,
        CenterEllipsesPipe,
        ColumnNameTranslatePipe
       ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SavedSearchesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
