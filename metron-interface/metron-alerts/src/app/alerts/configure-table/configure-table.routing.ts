import { ModuleWithProviders }  from '@angular/core';
import { RouterModule } from '@angular/router';
import {ConfigureTableComponent} from './configure-table.component';

export const routing: ModuleWithProviders = RouterModule.forChild([
    { path: 'configure-table', component: ConfigureTableComponent, outlet: 'dialog'}
]);
