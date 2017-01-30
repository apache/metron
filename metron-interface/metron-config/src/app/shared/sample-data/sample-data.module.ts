import {NgModule} from '@angular/core';

import {SampleDataComponent}   from './sample-data.component';
import {SharedModule} from '../shared.module';

@NgModule({
    imports: [ SharedModule ],
    exports: [ SampleDataComponent ],
    declarations: [ SampleDataComponent ],
    providers: [],
})
export class SampleDataModule {
}
