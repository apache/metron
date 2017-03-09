import {NgModule} from '@angular/core';

import {MetronSorterComponent} from './metron-sorter/metron-sorter.component';
import {MetronTableDirective} from './metron-table.directive';
import {SharedModule} from '../shared.module';

@NgModule({
    imports: [ SharedModule ],
    exports: [ MetronSorterComponent, MetronTableDirective ],
    declarations: [ MetronSorterComponent, MetronTableDirective ],
    providers: [],
})
export class MetronTableModule {
}
