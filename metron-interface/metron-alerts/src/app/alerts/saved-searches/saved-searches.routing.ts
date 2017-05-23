import { ModuleWithProviders }  from '@angular/core';
import { RouterModule } from '@angular/router';
import {SavedSearchesComponent} from './saved-searches.component';

export const routing: ModuleWithProviders = RouterModule.forChild([
  { path: 'saved-searches', component: SavedSearchesComponent, outlet: 'dialog'}
]);
