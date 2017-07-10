import {NgModule} from '@angular/core';

import {MultipleInputComponent}   from './multiple-input.component';
import {SharedModule} from '../shared.module';

@NgModule({
    imports: [ SharedModule ],
    exports: [ MultipleInputComponent ],
    declarations: [ MultipleInputComponent ],
    providers: [],
})
export class MultipleInputModule {
}
