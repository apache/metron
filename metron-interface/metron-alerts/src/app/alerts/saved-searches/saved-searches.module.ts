import { NgModule } from '@angular/core';

import {routing} from './saved-searches.routing';
import {SharedModule} from '../../shared/shared.module';
import {SavedSearchesComponent} from './saved-searches.component';
import {CollapseModule} from '../../shared/collapse/collapse.module';

@NgModule ({
  imports: [ routing,  SharedModule, CollapseModule],
  declarations: [ SavedSearchesComponent ],
  providers: [ ]
})
export class SavedSearchesModule { }
