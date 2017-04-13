import {NgModule} from '@angular/core';

import {AdvancedConfigFormComponent}   from './advanced-config-form.component';
import {SharedModule} from '../shared.module';
import {ReactiveFormsModule} from '@angular/forms';

@NgModule({
    imports: [ ReactiveFormsModule, SharedModule ],
    exports: [ AdvancedConfigFormComponent ],
    declarations: [ AdvancedConfigFormComponent ],
    providers: [],
})
export class AdvancedConfigFormModule {
}
