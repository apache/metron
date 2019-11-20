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
import { ComponentFixture, async, TestBed, fakeAsync, tick } from '@angular/core/testing';
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
import { Observable, Subject, of, throwError } from 'rxjs';
import { Filter } from 'app/model/filter';
import { QueryBuilder, FilteringMode } from './query-builder';
import { SearchResponse } from 'app/model/search-response';
import { AutoPollingService } from './auto-polling/auto-polling.service';
import { Router, NavigationStart } from '@angular/router';
import { Alert } from 'app/model/alert';
import { AlertSource } from 'app/model/alert-source';
import { SearchRequest } from 'app/model/search-request';
import { query } from '@angular/core/src/render3';
import { RestError } from 'app/model/rest-error';
import { DialogType } from 'app/shared/metron-dialog/metron-dialog.component';
import { DateFilterValue } from 'app/model/date-filter-value';
import { SaveSearch } from 'app/model/save-search';

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


fdescribe('AlertsListComponent', () => {

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
          updateAlertState: () => of(),
        } } },
        { provide: ConfigureTableService, useClass: () => { return {
          getTableMetadata: () => new Observable(),
          tableChanged$: new Observable(),
          saveTableMetaData: () => of(),
        } } },
        { provide: AlertsService, useClass: () => { return {} } },
        { provide: ClusterMetaDataService, useClass: () => { return {
          getDefaultColumns: () => new Observable(),
        } } },
        { provide: SaveSearchService, useClass: () => { return {
          loadSavedSearch$: new Observable(),
          setCurrentQueryBuilderAndTableColumns: () => {},
          fireLoadSavedSearch: (savedSearch: any) => {
            this.loadSavedSearch$.next(savedSearch);
          },
          saveAsRecentSearches: (savedSearch: any) => of(),
        } } },
        { provide: MetaAlertService, useClass: () => { return {
          alertChanged$: new Observable(),
          selectedAlerts: []
        } } },
        { provide: GlobalConfigService, useClass: () => { return {
          get: () => new Observable(),
        } } },
        { provide: DialogService, useClass: () => { return {} } },
        { provide: QueryBuilder, useClass: () => { return {
          filters: [],
          query: '*',
          searchRequest: () => {
            return new SearchResponse();
          },
          addOrUpdateFilter: () => {},
          clearSearch: () => {},
          isTimeStampFieldPresent: () => {},
          getManualQuery: () => {},
          setManualQuery: () => {},
          getFilteringMode: () => {},
          setFilteringMode: () => {},
          setSearch: () => {},
        } } },
        { provide: AutoPollingService, useClass: () => { return {
          data: new Subject<SearchResponse>(),
          getIsCongestion: () => {},
          getInterval: () => {},
          getIsPollingActive: () => {},
          dropNextAndContinue: () => {},
          onDestroy: () => {},
          setSuppression: () => {},
          setInterval: () => {},
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

  it('should get global config on init', () => {
    const globalConfig = { 'source.type.field': 'test'}
    const globalConfigService = TestBed.get(GlobalConfigService);
    spyOn(globalConfigService, 'get').and.returnValue(of(globalConfig));
    component.ngOnInit();
    component.alertsColumns = [
      { name: 'ip:source', type: 'testType' },
      { name: 'source:type', type: 'testType' }
    ];
    fixture.detectChanges();
    expect(component.globalConfig).toEqual(globalConfig);
    expect(component.alertsColumns.length).toEqual(2);
  });

  it('should clear selected alerts if NavigationStart', () => {
    const router = TestBed.get(Router);
    spyOn(router, 'events').and.returnValue({url: '/alerts-list'} instanceof NavigationStart);
    component.ngOnInit();
    fixture.detectChanges();
    expect(component.selectedAlerts).toEqual([]);
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
      spyOn(queryBuilder, 'setFilteringMode');

      queryBuilder.getFilteringMode = () => FilteringMode.BUILDER;

      component.toggleQueryBuilderMode();
      expect(queryBuilder.setFilteringMode).toHaveBeenCalledWith(FilteringMode.MANUAL);

      queryBuilder.getFilteringMode = () => FilteringMode.MANUAL;

      component.toggleQueryBuilderMode();
      expect(queryBuilder.setFilteringMode).toHaveBeenCalledWith(FilteringMode.BUILDER);
    });

    it('isQueryBuilderModeManual should return true if queryBuilder is in manual mode', () => {
      queryBuilder.getFilteringMode = () => FilteringMode.MANUAL;
      expect(component.isQueryBuilderModeManual()).toBe(true);

      queryBuilder.getFilteringMode = () => FilteringMode.BUILDER;
      expect(component.isQueryBuilderModeManual()).toBe(false);
    });

    it('should show manual input dom element depending on mode', () => {
      let input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));

      expect(input).toBeFalsy();

      queryBuilder.getFilteringMode = () => FilteringMode.MANUAL;
      fixture.detectChanges();
      input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));

      expect(input).toBeTruthy();

      queryBuilder.getFilteringMode = () => FilteringMode.BUILDER;
      fixture.detectChanges();
      input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));

      expect(input).toBeFalsy();
    });

    it('should bind default manual query from query builder', () => {
      spyOn(queryBuilder, 'getManualQuery').and.returnValue('test manual query string')

      queryBuilder.getFilteringMode = () => FilteringMode.MANUAL;
      fixture.detectChanges();
      let input: HTMLInputElement = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]')).nativeElement;

      expect(input.value).toBe('test manual query string');
    });

    it('should pass the manual query value to the query builder when editing mode is manual', fakeAsync(() => {
      spyOn(queryBuilder, 'setManualQuery');

      queryBuilder.getFilteringMode = () => FilteringMode.MANUAL;
      fixture.detectChanges();

      const input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));
      const el = input.nativeElement;

      el.value = 'test';
      (el as HTMLElement).dispatchEvent(new Event('keyup'));
      fixture.detectChanges();
      tick(300);

      expect(queryBuilder.setManualQuery).toHaveBeenCalledWith('test');
    }));
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

    it('should set stale date true when query changes in manual mode', fakeAsync(() => {
      queryBuilder.getFilteringMode = () => FilteringMode.MANUAL;

      fixture.detectChanges();
      const input = fixture.debugElement.query(By.css('[data-qe-id="manual-query-input"]'));
      const el = input.nativeElement;

      el.value = 'test';
      (el as HTMLElement).dispatchEvent(new Event('keyup'));
      fixture.detectChanges();
      tick(300);

      expect(component.staleDataState).toBe(true);
    }));

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

  describe('search', () => {
    it('should fire onSearch if in manual search mode', () => {
      const testQuery = 'test';
      spyOn(queryBuilder, 'setFilteringMode').and.returnValue(FilteringMode.BUILDER);
      spyOn(queryBuilder, 'setSearch');
      spyOn(component, 'search');
      component.toggleQueryBuilderMode();
      fixture.detectChanges();
      component.onSearch(testQuery);
      expect(queryBuilder.setSearch).toHaveBeenCalledWith(testQuery);
    });

    it('should show notification on http error', fakeAsync(() => {
      const fakeDialogService = TestBed.get(DialogService);

      spyOn(searchService, 'search').and.returnValue(throwError(new RestError()));
      fakeDialogService.launchDialog = () => {};
      spyOn(fakeDialogService, 'launchDialog');

      component.search();

      expect(fakeDialogService.launchDialog).toHaveBeenCalledWith('Server were unable to apply query string.', DialogType.Error);
    }));

    it('should save current search', () => {
      const saveSearchService = TestBed.get(SaveSearchService);
      const search = new SaveSearch();

      spyOn(saveSearchService, 'saveAsRecentSearches').and.callThrough();
      spyOn(queryBuilder, 'query').and.returnValue('test');
      component.saveCurrentSearch(search);
      expect(saveSearchService.saveAsRecentSearches).toHaveBeenCalledWith(search);
    });
  });

 describe('actions', () => {
    const testAlert: Alert = {
      id: '123',
      score: 123,
      source: new AlertSource(),
      status: 'TEST',
      index: 'test'
    }

    it('should prevent a user from selecting a new alert state if disabled', () => {
      const updateService = TestBed.get(UpdateService);
      spyOn(updateService, 'updateAlertState')
      spyOn(component, 'preventDropdownOptionIfDisabled');
      const actionsButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="dropdown-menu-button"');
      actionsButton.click();
      const openButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="action-open-button"');
      expect(openButton.classList).toContain('disabled');
      openButton.dispatchEvent(new Event('click'));
      expect(component.preventDropdownOptionIfDisabled).toHaveBeenCalled();
      expect(updateService.updateAlertState).not.toHaveBeenCalled();
    });


    it('should update the alert state to open when selected', () => {
      component.selectedAlerts.push(testAlert);
      fixture.detectChanges();
      const updateService = TestBed.get(UpdateService);
      spyOn(updateService, 'updateAlertState').and.returnValue(of({}));
      spyOn(component, 'processOpen').and.callThrough();
      spyOn(component, 'updateSelectedAlertStatus');
      const actionsButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="dropdown-menu-button"');
      actionsButton.click();
      const openButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="action-open-button"');
      openButton.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(component.processOpen).toHaveBeenCalled();
      expect(updateService.updateAlertState).toHaveBeenCalledWith(component.selectedAlerts, 'OPEN', false);
    });

    it('should update the alert state to dismiss when selected', () => {
      component.selectedAlerts.push(testAlert);
      fixture.detectChanges();
      const updateService = TestBed.get(UpdateService);
      spyOn(updateService, 'updateAlertState').and.returnValue(of());
      spyOn(component, 'processDismiss').and.callThrough();
      spyOn(component, 'updateSelectedAlertStatus');
      const actionsButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="dropdown-menu-button"');
      actionsButton.click();
      const dismissButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="action-dismiss-button"');
      dismissButton.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(component.processDismiss).toHaveBeenCalled();
      expect(updateService.updateAlertState).toHaveBeenCalledWith(component.selectedAlerts, 'DISMISS', false);
    });

    it('should update the alert state to escalate when selected', () => {
      component.selectedAlerts.push(testAlert);
      fixture.detectChanges();
      const updateService = TestBed.get(UpdateService);
      spyOn(updateService, 'updateAlertState').and.returnValue(of());
      spyOn(component, 'processEscalate').and.callThrough();
      spyOn(component, 'updateSelectedAlertStatus');
      const actionsButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="dropdown-menu-button"');
      actionsButton.click();
      const escalateButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="action-escalate-button"');
      escalateButton.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(component.processEscalate).toHaveBeenCalled();
      expect(updateService.updateAlertState).toHaveBeenCalledWith(component.selectedAlerts, 'ESCALATE', false);
    });

    it('should update the alert state to resolve when selected', () => {
      component.selectedAlerts.push(testAlert);
      fixture.detectChanges();
      const updateService = TestBed.get(UpdateService);
      spyOn(updateService, 'updateAlertState').and.returnValue(of());
      spyOn(component, 'processResolve').and.callThrough();
      spyOn(component, 'updateSelectedAlertStatus');
      const actionsButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="dropdown-menu-button"');
      actionsButton.click();
      const resolveButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="action-resolve-button"');
      resolveButton.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(component.processResolve).toHaveBeenCalled();
      expect(updateService.updateAlertState).toHaveBeenCalledWith(component.selectedAlerts, 'RESOLVE', false);
    });

    it('should add to alert when selected', () => {
      component.selectedAlerts.push(testAlert);
      fixture.detectChanges();
      const metaAlertService = TestBed.get(MetaAlertService);
      const router = TestBed.get(Router);
      spyOn(component, 'processAddToAlert').and.callThrough();
      spyOn(component, 'updateSelectedAlertStatus');
      spyOn(router, 'navigateByUrl');
      const actionsButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="dropdown-menu-button"');
      actionsButton.click();
      const processAddToAlertButton = fixture.debugElement.nativeElement.querySelector('[data-qe-id="action-add-to-alert-button"');
      processAddToAlertButton.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(component.processAddToAlert).toHaveBeenCalled();
      expect(metaAlertService.selectedAlerts).toEqual(component.selectedAlerts);
      expect(router.navigateByUrl).toHaveBeenCalledWith('/alerts-list(dialog:add-to-meta-alert)');
    });
  });

  describe('table configuration', () => {
    it('should reconfigure rows when value is emitted to configRowsChange', () => {
      const configureTableService = TestBed.get(ConfigureTableService);
      spyOn(component, 'updatePollingInterval');
      spyOn(component, 'search');
      spyOn(configureTableService, 'saveTableMetaData').and.callThrough();
      component.onConfigRowsChange({
        values: {
          pageSize: 10,
          refreshInterval: 1000
        },
        triggerQuery: true
      });

      expect(component.tableMetaData.size).toEqual(10);
      expect(configureTableService.saveTableMetaData).toHaveBeenCalledWith(component.tableMetaData);
      expect(component.search).toHaveBeenCalled();
    });
  });
});
