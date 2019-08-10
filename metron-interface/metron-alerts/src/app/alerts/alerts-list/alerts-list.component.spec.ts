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
import { SearchRequest } from 'app/model/search-request';
import { Observable, of, Subject } from 'rxjs';
import { Filter } from 'app/model/filter';
import { QueryBuilder } from './query-builder';
import { TIMESTAMP_FIELD_NAME } from 'app/utils/constants';
import { SearchResponse } from 'app/model/search-response';
import { By } from '@angular/platform-browser';

describe('AlertsListComponent', () => {

  let component: AlertsListComponent;
  let fixture: ComponentFixture<AlertsListComponent>;
  let searchServiceStub = {
    search() { return of({
      total: 0,
      groupedBy: '',
      results: [],
      facetCounts: [],
      groups: []
    }) },
    pollSearch() { return of({}) }
  }
  let queryBuilderStub = {
    addOrUpdateFilter() { return {} },
    clearSearch() { return {} },
    generateSelect() { return '*' },
    isTimeStampFieldPresent() { return {} },
    filters: [{}],
    searchRequest: {
      from: 0
    }
  }

  let queryBuilder: QueryBuilder;
  let searchService: SearchService;

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
        { provide: SearchService, useValue: searchServiceStub },
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
        { provide: QueryBuilder, useValue: queryBuilderStub },
      ]
    })
    .compileComponents();

    queryBuilder = TestBed.get(QueryBuilder);
    searchService = TestBed.get(SearchService);
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

  it('shows subtotals in view when onTreeViewChange is truthy', () => {
    component.onTreeViewChange(4);
    fixture.detectChanges();
    let subtotal = fixture.nativeElement.querySelector('[data-qe-id="alert-subgroup-total"]');
    expect(subtotal.textContent).toEqual('Alerts in Groups (4)');

    component.onTreeViewChange(0);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-qe-id="alert-subgroup-total"]')).toBeNull();
  });

  it('should toggle the query builder with toggleQueryBuilder', () => {
    component.toggleQueryBuilder();
    fixture.detectChanges();
    expect(component.hideQueryBuilder).toBe(true);

    component.hideQueryBuilder = true;
    component.pagination.from = 0;
    component.pagination.size = 25;

    fixture.detectChanges();
    component.toggleQueryBuilder();
    expect(component.hideQueryBuilder).toBe(false);
  });

  it('should pass the manual query value when hideQueryBuilder is true', () => {
    const input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));
    const el = input.nativeElement;

    expect(component.queryForTreeView()).toBe('*');

    component.toggleQueryBuilder();
    fixture.detectChanges();
    expect(component.hideQueryBuilder).toBe(true);

    el.value = 'test';
    expect(component.queryForTreeView()).toBe('test');
  });

  it('should build a new search request if hideQueryBuilder is true', () => {
    const input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));
    const el = input.nativeElement;
    const searchServiceSpy = spyOn(searchService, 'search').and.returnValue(of());
    const newSearch = new SearchRequest();

    el.value = 'test';
    component.hideQueryBuilder = true;
    component.pagination.size = 25;
    newSearch.query = 'test'
    newSearch.size = 25
    newSearch.from = 0;

    fixture.detectChanges();
    component.search();
    expect(searchServiceSpy).toHaveBeenCalledWith(newSearch);
  });

  it('should poll with new search request if isRefreshPaused is true and manualSearch is present', () => {
    const searchServiceSpy = spyOn(searchService, 'pollSearch').and.returnValue(of());
    const newSearch = new SearchRequest();

    component.isRefreshPaused = false;
    fixture.detectChanges();
    component.tryStartPolling(newSearch);
    expect(searchServiceSpy).toHaveBeenCalledWith(newSearch);
  });

  describe('stale data state', () => {

    it('should set staleDataState flag to true on filter change', () => {
      expect(component.staleDataState).toBe(false);
      component.onAddFilter(new Filter('ip_src_addr', '0.0.0.0'));
      expect(component.staleDataState).toBe(true);
    });

    it('should set staleDataState flag to true on filter clearing', () => {
      queryBuilder.clearSearch = jasmine.createSpy('clearSearch');

      expect(component.staleDataState).toBe(false);
      component.onClear();
      expect(component.staleDataState).toBe(true);
    });

    it('should set staleDataState flag to true on timerange change', () => {
      expect(component.staleDataState).toBe(false);
      component.onTimeRangeChange(new Filter(TIMESTAMP_FIELD_NAME, 'this-year'));
      expect(component.staleDataState).toBe(true);
    });

    it('should set staleDataState flag to false when the query resolves', () => {
      spyOn(searchService, 'search').and.returnValue(of(new SearchResponse()));
      spyOn(component, 'saveCurrentSearch');
      spyOn(component, 'setSearchRequestSize');
      spyOn(component, 'setSelectedTimeRange');
      spyOn(component, 'createGroupFacets');

      component.staleDataState = true;
      component.search();
      expect(component.staleDataState).toBe(false);
    });

    it('should show warning if data is in a stale state', () => {
      expect(fixture.debugElement.query(By.css('[data-qe-id="staleDataWarning"]'))).toBe(null);

      component.staleDataState = true;
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('[data-qe-id="staleDataWarning"]'))).toBeTruthy();
    });

  })

});
