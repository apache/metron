import { NgModule } from '@angular/core';
import {SharedModule} from '../../shared/shared.module';
import {ConfigureRowsComponent} from './configure-rows.component';
import {SwitchModule} from '../../shared/switch/switch.module';

@NgModule ({
    imports: [ SharedModule, SwitchModule ],
    declarations: [ ConfigureRowsComponent ],
    exports: [  ConfigureRowsComponent ]
})
export class ConfigureRowsModule { }
