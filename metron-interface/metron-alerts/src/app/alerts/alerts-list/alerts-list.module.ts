import {NgModule} from '@angular/core';

import {AlertsListComponent}   from './alerts-list.component';
import {routing} from './alerts-list.routing';
import {SharedModule} from '../../shared/shared.module';
import {AlertService} from '../../service/alert.service';
import {MetronSorterModule} from '../../shared/metron-table/metron-sorter/metron-sorter.module';
import {ListGroupModule} from '../../shared/list-group/list-grup.module';
import {CollapseModule} from '../../shared/collapse/collapse.module';
import {MetronTablePaginationModule} from '../../shared/metron-table/metron-table-pagination/metron-table-pagination.module';
import {ConfigureRowsModule} from '../configure-rows/configure-rows.module';

@NgModule({
    imports: [routing, SharedModule, ConfigureRowsModule, MetronSorterModule, MetronTablePaginationModule,
                ListGroupModule, CollapseModule],
    exports: [],
    declarations: [AlertsListComponent],
    providers: [AlertService],
})
export class AlertsListModule {
}
