import {NgModule} from '@angular/core';

import {CollapseComponent}   from './collapse.component';
import {SharedModule} from '../shared.module';

@NgModule({
  imports: [SharedModule],
  exports: [CollapseComponent],
  declarations: [CollapseComponent],
  providers: [],
})
export class CollapseModule {
}
