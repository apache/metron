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

import { ConfigureTableComponent } from './configure-table.component';
import { ConfigureTableService } from '../../service/configure-table.service';
import { SwitchComponent } from '../../shared/switch/switch.component';
import { CenterEllipsesPipe } from '../../shared/pipes/center-ellipses.pipe';
import { ClusterMetaDataService } from 'app/service/cluster-metadata.service';
import { SearchService } from 'app/service/search.service';
import { ColumnNamesService } from 'app/service/column-names.service';

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
    expect(component.filteredColumns.length).toBe(18);

    filter.value = 'guid';
    filter.dispatchEvent(new Event('keyup'));
    tick(300);
    fixture.detectChanges();
    expect(component.filteredColumns.length).toBe(1);
    expect(component.filteredColumns[0].columnMetadata.name).toBe('guid');

    filter.value = '';
    filter.dispatchEvent(new Event('keyup'));
    tick(300);
    fixture.detectChanges();
    expect(component.filteredColumns.length).toBe(18);
  }));

  it('should reset filter input and available columns when clear button is clicked', fakeAsync(() => {
    const filter = fixture.nativeElement.querySelector('[data-qe-id="filter-input"]');
    const filterReset = fixture.nativeElement.querySelector('[data-qe-id="filter-reset"]');

    component.ngOnInit();
    component.ngAfterViewInit();

    filter.value = 'guid';
    filter.dispatchEvent(new Event('keyup'));
    tick(300);
    fixture.detectChanges();
    expect(component.filteredColumns.length).toBe(1);

    filterReset.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(filter.value).toBe('');
    expect(component.filteredColumns.length).toBe(18);
  }));

  it('should filter by display name if display name is present', fakeAsync(() => {
    const filter = fixture.nativeElement.querySelector('[data-qe-id="filter-input"]');

    component.ngOnInit();
    component.ngAfterViewInit();
    expect(component.filteredColumns.length).toBe(18);

    component.filteredColumns[0].displayName = 'Test Display Name';

    filter.value = 'test';
    filter.dispatchEvent(new Event('keyup'));
    tick(300);
    fixture.detectChanges();
    expect(component.filteredColumns.length).toBe(1);
  }));

});
