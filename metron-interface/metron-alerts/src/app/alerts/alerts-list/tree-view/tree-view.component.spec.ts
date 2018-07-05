import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';

import { TreeViewComponent } from './tree-view.component';
import { AlertSeverityHexagonDirective } from '../../../shared/directives/alert-severity-hexagon.directive';
import { CenterEllipsesPipe } from '../../../shared/pipes/center-ellipses.pipe';
import { MetronTableDirective } from '../../../shared/metron-table/metron-table.directive';
import { MetronSorterComponent } from '../../../shared/metron-table/metron-sorter';
import { ColumnNameTranslatePipe } from '../../../shared/pipes/column-name-translate.pipe';
import { AlertSeverityDirective } from '../../../shared/directives/alert-severity.directive';
import { MetronTablePaginationComponent } from '../../../shared/metron-table/metron-table-pagination/metron-table-pagination.component';
import { SearchService } from '../../../service/search.service';
import { MetronDialogBox } from '../../../shared/metron-dialog-box';
import { UpdateService } from '../../../service/update.service';
import { GlobalConfigService } from '../../../service/global-config.service';
import { MetaAlertService } from '../../../service/meta-alert.service';

describe('TreeViewComponent', () => {
  let component: TreeViewComponent;
  let fixture: ComponentFixture<TreeViewComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientModule ],
      providers: [
        SearchService,
        UpdateService,
        GlobalConfigService,
        MetaAlertService,
        MetronDialogBox,
      ],
      declarations: [
        MetronTableDirective,
        MetronSorterComponent,
        MetronTablePaginationComponent,
        AlertSeverityHexagonDirective,
        AlertSeverityDirective,
        CenterEllipsesPipe,
        ColumnNameTranslatePipe,
        TreeViewComponent,
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TreeViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
