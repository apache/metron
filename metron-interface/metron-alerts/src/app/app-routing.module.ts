import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

const routes: Routes = [
  { path: '',  redirectTo: 'alerts-list', pathMatch: 'full'},
  { path: 'alerts-list', loadChildren: 'app/alerts/alerts-list/alerts-list.module#AlertsListModule'},
  { path: 'save-search', loadChildren: 'app/alerts/save-search/save-search.module#SaveSearchModule'},
  { path: 'saved-searches', loadChildren: 'app/alerts/saved-searches/saved-searches.module.ts#SavedSearchesModule'}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
  providers: []
})
export class MetronAlertsRoutingModule { }
