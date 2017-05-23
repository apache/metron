import { NgModule } from '@angular/core';

import {routing} from './save-search.routing';
import {SharedModule} from '../../shared/shared.module';
import {SaveSearchComponent} from './save-search.component';

@NgModule ({
  imports: [ routing,  SharedModule],
  declarations: [ SaveSearchComponent ],
  providers: [ ]
})
export class SaveSearchModule { }
