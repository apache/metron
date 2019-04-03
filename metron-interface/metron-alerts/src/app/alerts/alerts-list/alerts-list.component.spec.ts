/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { AlertsListComponent } from './alerts-list.component';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { SearchService } from 'app/service/search.service';
import { UpdateService } from 'app/service/update.service';
import { ConfigureTableService } from 'app/service/configure-table.service';
import { AlertsService } from 'app/service/alerts.service';
import { ClusterMetaDataService } from 'app/service/cluster-metadata.service';
import { SaveSearchService } from 'app/service/save-search.service';
import { MetaAlertService } from 'app/service/meta-alert.service';
import { GlobalConfigService } from 'app/service/global-config.service';
import { DialogService } from 'app/service/dialog.service';
import { Observable } from 'rxjs';
import { Filter } from 'app/model/filter';

describe('AlertsListComponent', () => {

  let component: AlertsListComponent;
  let fixture: ComponentFixture<AlertsListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      schemas: [ NO_ERRORS_SCHEMA ],
      imports: [
        RouterTestingModule.withRoutes([]),
      ],
      declarations: [
        AlertsListComponent,
      ],
      providers: [
        { provide: SearchService, useClass: () => { return {} } },
        { provide: UpdateService, useClass: () => { return {
          alertChanged$: new Observable(),
        } } },
        { provide: ConfigureTableService, useClass: () => { return {
          getTableMetadata: () => new Observable(),
          tableChanged$: new Observable(),
        } } },
        { provide: AlertsService, useClass: () => { return {} } },
        { provide: ClusterMetaDataService, useClass: () => { return {
          getDefaultColumns: () => new Observable(),
        } } },
        { provide: SaveSearchService, useClass: () => { return {
          loadSavedSearch$: new Observable(),
        } } },
        { provide: MetaAlertService, useClass: () => { return {
          alertChanged$: new Observable(),
        } } },
        { provide: GlobalConfigService, useClass: () => { return {
          get: () => new Observable(),
        } } },
        { provide: DialogService, useClass: () => { return {} } },
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AlertsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should exist', () => {
    expect(component).toBeTruthy();
  });

  it('should set default query time range', () => {
    expect(component.selectedTimeRange instanceof Filter).toBeTruthy();
    expect(component.selectedTimeRange.value).toBe('last-15-minutes');
  });

  it('default query time range from date should be set', () => {
    expect(component.selectedTimeRange.dateFilterValue.fromDate).toBeTruthy();
  });

  it('default query time range to date should be set', () => {
    expect(component.selectedTimeRange.dateFilterValue.toDate).toBeTruthy();
  });

});
