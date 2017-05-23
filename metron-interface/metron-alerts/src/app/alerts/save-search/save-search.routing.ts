import { ModuleWithProviders }  from '@angular/core';
import { RouterModule } from '@angular/router';
import {SaveSearchComponent} from './save-search.component';

export const routing: ModuleWithProviders = RouterModule.forChild([
  { path: 'save-search', component: SaveSearchComponent, outlet: 'dialog'}
]);
