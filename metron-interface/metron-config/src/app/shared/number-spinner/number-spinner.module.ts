import {NgModule} from '@angular/core';

import {NumberSpinnerComponent}   from './number-spinner.component';
import {SharedModule} from '../shared.module';

@NgModule({
    imports: [ SharedModule ],
    exports: [ NumberSpinnerComponent ],
    declarations: [ NumberSpinnerComponent ],
    providers: [],
})
export class NumberSpinnerModule {
}
