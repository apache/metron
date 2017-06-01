import { ModuleWithProviders }  from '@angular/core';
import { RouterModule } from '@angular/router';
import {AlertDetailsComponent} from './alert-details.component';

export const routing: ModuleWithProviders = RouterModule.forChild([
    { path: 'details/:index/:type/:id', component: AlertDetailsComponent, outlet: 'dialog'}
]);
