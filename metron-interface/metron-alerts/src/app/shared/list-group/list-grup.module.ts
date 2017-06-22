import {NgModule} from '@angular/core';

import {ListGroupComponent}   from './list-group.component';
import {SharedModule} from '../shared.module';

@NgModule({
  imports: [SharedModule],
  exports: [ListGroupComponent],
  declarations: [ListGroupComponent],
  providers: [],
})
export class ListGroupModule {
}
