import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, Input } from '@angular/core';
import { HttpClient, HttpClientModule } from '@angular/common/http';

import { TableViewComponent } from './table-view.component';
import { MetronTableDirective } from '../../../shared/metron-table/metron-table.directive';
import { MetronSorterComponent } from '../../../shared/metron-table/metron-sorter';
import { CenterEllipsesPipe } from '../../../shared/pipes/center-ellipses.pipe';
import { ColumnNameTranslatePipe } from '../../../shared/pipes/column-name-translate.pipe';
import { AlertSeverityDirective } from '../../../shared/directives/alert-severity.directive';
import { MetronDialogBox } from '../../../shared/metron-dialog-box';
import { SearchService } from '../../../service/search.service';
import { UpdateService } from '../../../service/update.service';
import { GlobalConfigService } from '../../../service/global-config.service';
import { MetaAlertService } from '../../../service/meta-alert.service';

@Component({selector: 'metron-table-pagination', template: ''})
class MetronTablePaginationComponent {
  @Input() pagination = 0;
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
        MetronDialogBox,
        HttpClient,
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
