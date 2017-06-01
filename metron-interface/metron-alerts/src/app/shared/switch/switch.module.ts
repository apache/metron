import {NgModule} from '@angular/core';

import {SharedModule} from '../shared.module';
import {SwitchComponent} from './switch.component';

@NgModule({
  imports: [SharedModule],
  exports: [SwitchComponent],
  declarations: [SwitchComponent],
  providers: [],
})
export class SwitchModule {
}
