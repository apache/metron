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
import { Component, Input } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

import { TableViewComponent } from './table-view.component';
import { MetronTableDirective } from '../../../shared/metron-table/metron-table.directive';
import { MetronSorterComponent } from '../../../shared/metron-table/metron-sorter';
import { CenterEllipsesPipe } from '../../../shared/pipes/center-ellipses.pipe';
import { ColumnNameTranslatePipe } from '../../../shared/pipes/column-name-translate.pipe';
import { AlertSeverityDirective } from '../../../shared/directives/alert-severity.directive';
import { SearchService } from '../../../service/search.service';
import { UpdateService } from '../../../service/update.service';
import { GlobalConfigService } from '../../../service/global-config.service';
import { MetaAlertService } from '../../../service/meta-alert.service';
import { DialogService } from 'app/service/dialog.service';
import { AppConfigService } from '../../../service/app-config.service';

@Component({selector: 'metron-table-pagination', template: ''})
class MetronTablePaginationComponent {
  @Input() pagination = 0;
}

class FakeAppConfigService {

  getApiRoot() {
    return '/api/v1'
  }
}

describe('TableViewComponent', () => {
  let component: TableViewComponent;
  let fixture: ComponentFixture<TableViewComponent>;

  beforeEach(async(() => {
    // FIXME: mock all the unnecessary dependencies
    TestBed.configureTestingModule({
      imports: [ HttpClientModule ],
      providers: [
        SearchService,
        UpdateService,
        GlobalConfigService,
        MetaAlertService,
        DialogService,
        { provide: AppConfigService, useClass: FakeAppConfigService }
      ],
      declarations: [
        MetronTableDirective,
        MetronSorterComponent,
        CenterEllipsesPipe,
        ColumnNameTranslatePipe,
        AlertSeverityDirective,
        MetronTablePaginationComponent,
        TableViewComponent,
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TableViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
