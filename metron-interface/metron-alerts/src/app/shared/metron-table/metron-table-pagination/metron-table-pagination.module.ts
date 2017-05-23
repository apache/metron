import {NgModule} from '@angular/core';

import {MetronTablePaginationComponent}   from './metron-table-pagination.component';
import {SharedModule} from '../../shared.module';

@NgModule({
  imports: [SharedModule],
  exports: [MetronTablePaginationComponent],
  declarations: [MetronTablePaginationComponent],
  providers: [],
})
export class MetronTablePaginationModule {
}
