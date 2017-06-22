import {NgModule} from '@angular/core';

import {MetronSorterComponent}   from './metron-sorter.component';
import {SharedModule} from '../../shared.module';

@NgModule({
    imports: [SharedModule],
    exports: [MetronSorterComponent],
    declarations: [MetronSorterComponent],
    providers: [],
})
export class MetronSorterModule {
}
