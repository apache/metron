import { Router } from '@angular/router';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { AppComponent } from './app.component';
import {MetronAlertsRoutingModule} from './app-routing.module';
import {AlertsListModule} from './alerts/alerts-list/alerts-list.module';
import {AlertDetailsModule} from './alerts/alert-details/alerts-details.module';
import {APP_CONFIG, METRON_REST_CONFIG} from './app.config';
import {ConfigureTableModule} from './alerts/configure-table/configure-table.module';
import {ConfigureTableService} from './service/configure-table.service';
import {SaveSearchModule} from './alerts/save-search/save-search.module';
import {SaveSearchService} from './service/save-search.service';
import {SavedSearchesModule} from './alerts/saved-searches/saved-searches.module';
import {MetronDialogBox} from './shared/metron-dialog-box';
import {ConfigureRowsModule} from './alerts/configure-rows/configure-rows.module';
import {SwitchModule} from './shared/switch/switch.module';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    MetronAlertsRoutingModule,
    AlertsListModule,
    AlertDetailsModule,
    ConfigureTableModule,
    ConfigureRowsModule,
    SaveSearchModule,
    SavedSearchesModule,
    SwitchModule
  ],
  providers: [{ provide: APP_CONFIG, useValue: METRON_REST_CONFIG },
              ConfigureTableService,
              SaveSearchService,
              MetronDialogBox],
  bootstrap: [AppComponent]
})

export class AppModule {
  constructor(router: Router) {
  }
}
