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
import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { ConfigureTableComponent, ColumnMetadataWrapper } from './configure-table.component';
import { ConfigureTableService } from '../../service/configure-table.service';
import { SwitchComponent } from '../../shared/switch/switch.component';
import { CenterEllipsesPipe } from '../../shared/pipes/center-ellipses.pipe';
import { ClusterMetaDataService } from 'app/service/cluster-metadata.service';
import { SearchService } from 'app/service/search.service';
import { ColumnNamesService } from 'app/service/column-names.service';
import { By } from '@angular/platform-browser';

class FakeClusterMetaDataService {
  getDefaultColumns() {
    return of([
      { name: 'guid', type: 'string' },
      { name: 'timestamp', type: 'date' },
      { name: 'source:type', type: 'string' },
      { name: 'ip_src_addr', type: 'ip' },
      { name: 'enrichments:geo:ip_dst_addr:country', type: 'string' },
      { name: 'ip_dst_addr', type: 'ip' },
      { name: 'host', type: 'string' },
      { name: 'alert_status', type: 'string' }
    ]);
  }
}

class FakeSearchService {
  getColumnMetaData() {
    return of([
      { name: 'TTLs', type: 'TEXT' },
      { name: 'bro_timestamp', type: 'TEXT' },
      { name: 'enrichments:geo:ip_dst_addr:location_point', type: 'OTHER' },
      { name: 'sha256', type: 'KEYWORD' },
      { name: 'remote_location:city', type: 'TEXT' },
      { name: 'extracted', type: 'KEYWORD' },
      { name: 'parallelenricher:enrich:end:ts', type: 'DATE' },
      { name: 'certificate:version', type: 'INTEGER' },
      { name: 'path', type: 'KEYWORD' },
      { name: 'rpkt', type: 'INTEGER' }
    ]);

  }
}

class FakeConfigureTableService {
  getTableMetadata() {
    return of({
      hideDismissedAlerts: true,
      hideResolvedAlerts: true,
      refreshInterval: 60,
      size: 25
    });
  }
}

class FakeColumnNamesService {

}

describe('ConfigureTableComponent', () => {
  let component: ConfigureTableComponent;
  let fixture: ComponentFixture<ConfigureTableComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ FormsModule, RouterTestingModule ],
      declarations: [
        ConfigureTableComponent,
        SwitchComponent,
        CenterEllipsesPipe
      ],
      providers: [
        { provide: ClusterMetaDataService, useClass: FakeClusterMetaDataService },
        { provide: SearchService, useClass: FakeSearchService },
        { provide: ConfigureTableService, useClass: FakeConfigureTableService },
        { provide: ColumnNamesService, useClass: FakeColumnNamesService },
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ConfigureTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should filter available columns when input is present', fakeAsync(() =>  {
    const filter = fixture.nativeElement.querySelector('[data-qe-id="filter-input"]');

    component.ngOnInit();
    component.ngAfterViewInit();
    expect(component.filteredColumns.length).toBe(10);

    filter.value = 'timestamp';
    filter.dispatchEvent(new Event('keyup'));
    tick(300);
    fixture.detectChanges();
    expect(component.filteredColumns.length).toBe(1);
    expect(component.filteredColumns[0].columnMetadata.name).toBe('bro_timestamp');

    filter.value = '';
    filter.dispatchEvent(new Event('keyup'));
    tick(300);
    fixture.detectChanges();
    expect(component.filteredColumns.length).toBe(10);
  }));

  it('should reset filter input and available columns when clear button is clicked', fakeAsync(() => {
    const filter = fixture.nativeElement.querySelector('[data-qe-id="filter-input"]');
    const filterReset = fixture.nativeElement.querySelector('[data-qe-id="filter-reset"]');

    component.ngOnInit();
    component.ngAfterViewInit();

    filter.value = 'timestamp';
    filter.dispatchEvent(new Event('keyup'));
    tick(300);
    fixture.detectChanges();
    expect(component.filteredColumns.length).toBe(1);

    filterReset.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(filter.value).toBe('');
    expect(component.filteredColumns.length).toBe(10);
  }));

  it('should mark default columns as visible', () => {
    expect(component.visibleColumns.length).toBe(8);
  });

  it('should mark other columns as available', () => {
    expect(component.availableColumns.length).toBe(10);
  });

  it('should mark added element as selected', () => {
    const itemToAdd: ColumnMetadataWrapper = component.availableColumns[0];

    expect(itemToAdd.selected).toEqual(false);

    component.onColumnAdded(itemToAdd);

    expect(itemToAdd.selected).toEqual(true);
  });

  it('should mark removed element as deselected', () => {
    const itemToRemove: ColumnMetadataWrapper = component.visibleColumns[0];

    expect(itemToRemove.selected).toEqual(true);

    component.onColumnRemoved(itemToRemove);

    expect(itemToRemove.selected).toEqual(false);
  });

  it('should update list of visible items on add', () => {
    const itemToAdd: ColumnMetadataWrapper = component.availableColumns[0];

    expect(component.visibleColumns.includes(itemToAdd)).toEqual(false);

    component.onColumnAdded(itemToAdd);

    expect(component.visibleColumns.includes(itemToAdd)).toEqual(true);
  });

  it('should update list of available items on add', () => {
    const itemToAdd: ColumnMetadataWrapper = component.availableColumns[0];

    expect(component.availableColumns.includes(itemToAdd)).toEqual(true);

    component.onColumnAdded(itemToAdd);

    expect(component.availableColumns.includes(itemToAdd)).toEqual(false);
  });

  it('should update list of visible items on remove', () => {
    const itemToRemove: ColumnMetadataWrapper = component.visibleColumns[0];

    expect(component.visibleColumns.includes(itemToRemove)).toEqual(true);

    component.onColumnRemoved(itemToRemove);

    expect(component.visibleColumns.includes(itemToRemove)).toEqual(false);
  });

  it('should update list of available items on remove', () => {
    const itemToRemove: ColumnMetadataWrapper = component.visibleColumns[0];

    expect(component.availableColumns.includes(itemToRemove)).toEqual(false);

    component.onColumnRemoved(itemToRemove);

    expect(component.availableColumns.includes(itemToRemove)).toEqual(true);
  });

  it('should sort available items on change', () => {
    spyOn(component.availableColumns, 'sort');

    const itemToRemove: ColumnMetadataWrapper = component.visibleColumns[0];
    component.onColumnRemoved(itemToRemove);

    expect(component.availableColumns.sort).toHaveBeenCalled();
  });

  describe('Config Pane Rendering', () => {
    it('should render visible and available items separately', () => {
      expect(fixture.debugElement.queryAll(By.css('table')).length).toBe(2);
      expect(fixture.debugElement.queryAll(By.css('table'))[0].queryAll(By.css('tr')).length).toBe(10);
      expect(fixture.debugElement.queryAll(By.css('table'))[1].queryAll(By.css('tr')).length).toBe(11);
    });

    it('should refresh both list on remove', () => {
      fixture.debugElement.query(By.css('[data-qe-id="remove-btn-1"]')).nativeElement.click();
      fixture.detectChanges();
      expect(fixture.debugElement.queryAll(By.css('table'))[0].queryAll(By.css('tr')).length).toBe(9);
      expect(fixture.debugElement.queryAll(By.css('table'))[1].queryAll(By.css('tr')).length).toBe(12);
    });

    it('should refresh both list on add', () => {
      fixture.debugElement.query(By.css('[data-qe-id="add-btn-4"]')).nativeElement.click();
      fixture.detectChanges();
      expect(fixture.debugElement.queryAll(By.css('table'))[0].queryAll(By.css('tr')).length).toBe(11);
      expect(fixture.debugElement.queryAll(By.css('table'))[1].queryAll(By.css('tr')).length).toBe(10);
    });

    it('should be able to move visible item DOWN in order', () => {
      const origIndex = 2;
      const newIndex = 3;
      let tableOfVisible = fixture.debugElement.query(By.css('[data-qe-id="table-visible"]'));
      const rowId = tableOfVisible.query(By.css(`[data-qe-id="field-label-${origIndex}"]`)).nativeElement.innerText;

      tableOfVisible.query(By.css(`[data-qe-id="row-${origIndex}"]`)).query(By.css('span[id^="down-"]')).nativeElement.click();
      fixture.detectChanges();

      tableOfVisible = fixture.debugElement.query(By.css('[data-qe-id="table-visible"]'));
      expect(tableOfVisible.query(By.css(`[data-qe-id="field-label-${newIndex}"]`)).nativeElement.innerText).toBe(rowId);
    });

    it('should be able to move visible item UP in order', () => {
      const origIndex = 3;
      const newIndex = 2;
      let tableOfVisible = fixture.debugElement.query(By.css('[data-qe-id="table-visible"]'));
      const rowId = tableOfVisible.query(By.css(`[data-qe-id="field-label-${origIndex}"]`)).nativeElement.innerText;

      tableOfVisible.query(By.css(`[data-qe-id="row-${origIndex}"]`)).query(By.css('span[id^="up-"]')).nativeElement.click();
      fixture.detectChanges();

      tableOfVisible = fixture.debugElement.queryAll(By.css('table'))[0];
      expect(tableOfVisible.query(By.css(`[data-qe-id="field-label-${newIndex}"]`)).nativeElement.innerText).toBe(rowId);
    });
  });
});
