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
import { ComponentFixture, async, TestBed, fakeAsync } from '@angular/core/testing';
import { Component, Input, Directive } from '@angular/core';
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
import { TIMESTAMP_FIELD_NAME } from 'app/utils/constants';
import { By } from '@angular/platform-browser';
import { Observable, Subject, of } from 'rxjs';
import { Filter } from 'app/model/filter';
import { QueryBuilder, FilteringMode } from './query-builder';
import { SearchResponse } from 'app/model/search-response';
import { AutoPollingService } from './auto-polling/auto-polling.service';
import { Router } from '@angular/router';
import { Alert } from 'app/model/alert';
import { AlertSource } from 'app/model/alert-source';
import { SearchRequest } from 'app/model/search-request';

@Component({
  selector: 'app-auto-polling',
  template: '<div></div>',
})
class MockAutoPollingComponent {}

@Component({
  selector: 'app-configure-rows',
  template: '<div></div>',
})
class MockConfigureRowsComponent {
  @Input() refreshInterval = 0;
  @Input() srcElement = {};
  @Input() pageSize = 0;
}

@Component({
  selector: 'app-modal-loading-indicator',
  template: '<div></div>',
})
class MockModalLoadingIndicatorComponent {
  @Input() show = false;
}

@Component({
  selector: 'app-time-range',
  template: '<div></div>',
})
class MockTimeRangeComponent {
  @Input() disabled = false;
  @Input() selectedTimeRange = {};
}

@Directive({
  selector: '[appAceEditor]',
})
class MockAceEditorDirective {
  @Input() text = '';
}

@Component({
  selector: 'app-alert-filters',
  template: '<div></div>',
})
class MockAlertFilterComponent {
  @Input() facets = [];
}

@Component({
  selector: 'app-group-by',
  template: '<div></div>',
})
class MockGroupByComponent {
  @Input() facets = [];
}

@Component({
  selector: 'app-table-view',
  template: '<div></div>',
})
class MockTableViewComponent {
  @Input() alerts = [];
  @Input() pagination = {};
  @Input() alertsColumnsToDisplay = [];
  @Input() selectedAlerts = [];
}

@Component({
  selector: 'app-tree-view',
  template: '<div></div>',
})
class MockTreeViewComponent {
  @Input() alerts = [];
  @Input() pagination = {};
  @Input() alertsColumnsToDisplay = [];
  @Input() selectedAlerts = [];
  @Input() globalConfig = {};
  @Input() query = '';
  @Input() groups = [];
}


describe('AlertsListComponent', () => {

  let component: AlertsListComponent;
  let fixture: ComponentFixture<AlertsListComponent>;

  let queryBuilder: QueryBuilder;
  let searchService: SearchService;

  beforeEach(async(() => {

    const searchResponseFake = new SearchResponse();
    searchResponseFake.facetCounts = {};

    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([{path: 'alerts-list', component: AlertsListComponent}]),
      ],
      declarations: [
        AlertsListComponent,
        MockAutoPollingComponent,
        MockModalLoadingIndicatorComponent,
        MockTimeRangeComponent,
        MockAceEditorDirective,
        MockConfigureRowsComponent,
        MockAlertFilterComponent,
        MockGroupByComponent,
        MockTableViewComponent,
        MockTreeViewComponent,
      ],
      providers: [
        { provide: SearchService, useClass: () => { return {
          search: () => of(searchResponseFake),
        } } },
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
          setCurrentQueryBuilderAndTableColumns: () => {},
        } } },
        { provide: MetaAlertService, useClass: () => { return {
          alertChanged$: new Observable(),
        } } },
        { provide: GlobalConfigService, useClass: () => { return {
          get: () => new Observable(),
        } } },
        { provide: DialogService, useClass: () => { return {} } },
        { provide: QueryBuilder, useClass: () => { return {
          addOrUpdateFilter: () => {},
          clearSearch: () => {},
          generateSelect: () => {},
          isTimeStampFieldPresent: () => {},
          filteringMode: FilteringMode.BUILDER,
          filters: [],
          query: '*'
        } } },
        { provide: AutoPollingService, useClass: () => { return {
          data: new Subject<SearchResponse>(),
          getIsCongestion: () => {},
          getInterval: () => {},
          getIsPollingActive: () => {},
          dropNextAndContinue: () => {},
          onDestroy: () => {},
          setSuppression: () => {},
        } } },
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

  describe('filtering by query builder or manual query', () => {
    it('should be able to toggle the query builder mode', () => {
      spyOn(component, 'setSearchRequestSize');

      expect(component.isQueryBuilderModeManual()).toBe(false);

      component.toggleQueryBuilderMode();
      expect(component.isQueryBuilderModeManual()).toBe(true);

      component.toggleQueryBuilderMode();
      expect(component.isQueryBuilderModeManual()).toBe(false);
    });

    it('should pass the manual query value when mode is manual', () => {
      const input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));
      const el = input.nativeElement;

      const queryBuilderFake = TestBed.get(QueryBuilder);
      spyOn(queryBuilderFake, 'generateSelect').and.returnValue('*');

      expect(component.queryForTreeView()).toBe('*');

      component.toggleQueryBuilderMode();
      expect(component.isQueryBuilderModeManual()).toBe(true);

      el.value = 'test';
      expect(component.queryForTreeView()).toBe('test');
    });

    it('should use manual query string on search if mode is manual', () => {
      const input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));
      const el = input.nativeElement;
      const newSearch = new SearchRequest();

      spyOn(searchService, 'search').and.returnValue(of());
      spyOn(component, 'setSearchRequestSize');

      component.toggleQueryBuilderMode();

      el.value = 'test';
      component.pagination.size = 25;
      newSearch.query = 'test'
      newSearch.size = 25
      newSearch.from = 0;

      fixture.detectChanges();
      component.search();
      expect(searchService.search).toHaveBeenCalledWith(newSearch);
    });

    xit('should poll with new search request if isRefreshPaused is true and manualSearch is present', () => {
      // const searchServiceSpy = spyOn(searchService, 'pollSearch').and.returnValue(of());
      // const newSearch = new SearchRequest();

      // component.isRefreshPaused = false;
      // fixture.detectChanges();
      // component.tryStartPolling(newSearch);
      // expect(searchServiceSpy).toHaveBeenCalledWith(newSearch);
    });
  });

  describe('handling pending search requests', () => {
    it('should set pendingSearch on search', () => {
      spyOn(searchService, 'search').and.returnValue(of(new SearchResponse()));
      spyOn(component, 'saveCurrentSearch');
      spyOn(component, 'setSearchRequestSize');
      spyOn(component, 'setSelectedTimeRange');
      spyOn(component, 'createGroupFacets');

      component.search();
      expect(component.pendingSearch).toBeTruthy();
    });

    it('should clear pendingSearch on search success', (done) => {
      const fakeObservable = new Subject();
      spyOn(searchService, 'search').and.returnValue(fakeObservable);
      spyOn(component, 'saveCurrentSearch');
      spyOn(component, 'setSearchRequestSize');
      spyOn(component, 'setSelectedTimeRange');
      spyOn(component, 'createGroupFacets');

      component.search();

      setTimeout(() => {
        fakeObservable.next(new SearchResponse());
      }, 0);

      fakeObservable.subscribe(() => {
        expect(component.pendingSearch).toBe(null);
        done();
      })
    });
  });

  describe('stale data state', () => {

    it('should set staleDataState flag to true on filter change', () => {
      expect(component.staleDataState).toBe(false);
      component.onAddFilter(new Filter('ip_src_addr', '0.0.0.0'));
      expect(component.staleDataState).toBe(true);
    });

    it('should set staleDataState flag to true on filter clearing', () => {
      spyOn(component, 'setSearchRequestSize');

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

  });

  describe('auto polling', () => {

    it('should refresh view on data emit', () => {
      const fakeResponse = new SearchResponse();
      spyOn(component, 'setData');

      TestBed.get(AutoPollingService).data.next(fakeResponse);

      expect(component.setData).toHaveBeenCalledWith(fakeResponse);
    });

    it('should set staleDataState false on auto polling refresh', () => {
      spyOn(component, 'setData');
      component.staleDataState = true;

      TestBed.get(AutoPollingService).data.next(new SearchResponse());

      expect(component.staleDataState).toBe(false);
    });

    it('should show warning on auto polling congestion', () => {
      expect(fixture.debugElement.query(By.css('[data-qe-id="pollingCongestionWarning"]'))).toBeFalsy();

      TestBed.get(AutoPollingService).getIsCongestion = () => true;
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('[data-qe-id="pollingCongestionWarning"]'))).toBeTruthy();

      TestBed.get(AutoPollingService).getIsCongestion = () => false;
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('[data-qe-id="pollingCongestionWarning"]'))).toBeFalsy();
    });

    it('should pass refresh interval to row config component', () => {
      TestBed.get(AutoPollingService).getInterval = () => 44;
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.directive(MockConfigureRowsComponent)).componentInstance.refreshInterval).toBe(44);
    });

    it('should drop pending auto polling result if user trigger search request manually', () => {
      const autoPollingSvc = TestBed.get(AutoPollingService);
      spyOn(autoPollingSvc, 'dropNextAndContinue');
      spyOn(component, 'setSearchRequestSize');

      autoPollingSvc.getIsPollingActive = () => false;
      component.search()

      expect(autoPollingSvc.dropNextAndContinue).not.toHaveBeenCalled();

      autoPollingSvc.getIsPollingActive = () => true;
      component.search()

      expect(autoPollingSvc.dropNextAndContinue).toHaveBeenCalled();
    });

    it('should show different stale data warning when polling is active', () => {
      const autoPollingSvc = TestBed.get(AutoPollingService);

      autoPollingSvc.getIsPollingActive = () => false;
      const warning = component.getStaleDataWarning();

      autoPollingSvc.getIsPollingActive = () => true;
      const warningWhenPolling = component.getStaleDataWarning();

      expect(warning).not.toEqual(warningWhenPolling);
    });

    it('should show getIsCongestion scennarios', () => {
      const autoPollingSvc = TestBed.get(AutoPollingService);

      autoPollingSvc.getIsCongestion = () => false;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('[data-qe-id="pollingCongestionWarning"]'))).toBeFalsy();

      autoPollingSvc.getIsCongestion = () => true;
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('[data-qe-id="pollingCongestionWarning"]'))).toBeTruthy();

    });

    it('should suppress polling when user select alerts', () => {
      const autoPollingSvc = TestBed.get(AutoPollingService);
      spyOn(autoPollingSvc, 'setSuppression');

      component.onSelectedAlertsChange([{ source: { metron_alert: [] } }]);

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(true);
    });

    it('should restore polling from suppression when user deselect alerts', () => {
      const autoPollingSvc = TestBed.get(AutoPollingService);
      spyOn(autoPollingSvc, 'setSuppression');

      component.onSelectedAlertsChange([]);

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(false);
    });

    it('should suppress polling when open details pane', () => {
      const autoPollingSvc = TestBed.get(AutoPollingService);
      const router = TestBed.get(Router);
      spyOn(router, 'navigate').and.returnValue(true);
      spyOn(router, 'navigateByUrl').and.returnValue(true);
      spyOn(autoPollingSvc, 'setSuppression');

      component.showConfigureTable();

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(true);
    });

    it('should suppress polling when open column config pane', () => {
      const router = TestBed.get(Router);
      const autoPollingSvc = TestBed.get(AutoPollingService);
      spyOn(router, 'navigate');
      spyOn(router, 'navigateByUrl');
      spyOn(autoPollingSvc, 'setSuppression');

      const fakeAlert = new Alert();
      fakeAlert.source = new AlertSource();

      component.showDetails(fakeAlert);

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(true);
    });

    it('should suppress polling when open Saved Searches pane', () => {
      const router = TestBed.get(Router);
      const autoPollingSvc = TestBed.get(AutoPollingService);
      spyOn(router, 'navigate');
      spyOn(router, 'navigateByUrl');
      spyOn(autoPollingSvc, 'setSuppression');

      component.showSavedSearches();

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(true);
    });

    it('should suppress polling when open Save Search dialogue pane', () => {
      const router = TestBed.get(Router);
      const autoPollingSvc = TestBed.get(AutoPollingService);
      const saveSearchSvc = TestBed.get(SaveSearchService);
      spyOn(router, 'navigate');
      spyOn(router, 'navigateByUrl');
      spyOn(autoPollingSvc, 'setSuppression');
      spyOn(saveSearchSvc, 'setCurrentQueryBuilderAndTableColumns');

      component.showSaveSearch();

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(true);
    });

    it('should restore the polling supression on bulk status update (other scenario of deselecting alerts)', () => {
      const autoPollingSvc = TestBed.get(AutoPollingService);
      spyOn(autoPollingSvc, 'setSuppression');

      component.updateSelectedAlertStatus('fakeState');

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(false);
    });

    it('should restore the polling supression when returning from a subroute', fakeAsync(() => {
      const autoPollingSvc = TestBed.get(AutoPollingService);
      spyOn(autoPollingSvc, 'setSuppression');

      autoPollingSvc.getIsPollingActive = () => false;
      fixture.ngZone.run(() => {
        TestBed.get(Router).navigate(['/alerts-list']);
      });

      expect(autoPollingSvc.setSuppression).not.toHaveBeenCalled();

      autoPollingSvc.getIsPollingActive = () => true;
      fixture.ngZone.run(() => {
        TestBed.get(Router).navigate(['/alerts-list']);
      });

      expect(autoPollingSvc.setSuppression).toHaveBeenCalledWith(false);
    }));

  });
});
