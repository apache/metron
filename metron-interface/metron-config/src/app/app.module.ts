/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { StoreModule, MetaReducer } from '@ngrx/store';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { EffectsModule } from '@ngrx/effects'
import { SensorsModule } from './sensors/sensors.module';
import { storeFreeze } from 'ngrx-store-freeze';
import { environment } from '../environments/environment';

import { APP_INITIALIZER, NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AppComponent } from './app.component';
import { MetronAlerts } from './shared/metron-alerts';
import { NavbarComponent } from './navbar/navbar.component';
import { VerticalNavbarComponent } from './verticalnavbar/verticalnavbar.component';
import { MetronConfigRoutingModule } from './app.routes';
import { AuthenticationService } from './service/authentication.service';
import { AuthGuard } from './shared/auth-guard';
import { LoginGuard } from './shared/login-guard';
import { MetronDialogBox } from './shared/metron-dialog-box';
import { GeneralSettingsModule } from './general-settings/general-settings.module';
import { GlobalConfigService } from './service/global-config.service';
import { DefaultHeadersInterceptor } from './http-interceptors/default-headers.interceptor';
import {AppConfigService } from './service/app-config.service';

export const metaReducers: MetaReducer<{}>[] = !environment.production
? [storeFreeze]
: [];

export function initConfig(appConfigService: AppConfigService) {
  return () => appConfigService.loadAppConfig();
}

@NgModule({
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    GeneralSettingsModule,
    MetronConfigRoutingModule,
    SensorsModule,
    EffectsModule.forRoot([]),
    StoreModule.forRoot({}, { metaReducers }),
    StoreDevtoolsModule.instrument(),
  ],
  declarations: [ AppComponent, NavbarComponent, VerticalNavbarComponent ],
  providers: [
    AppConfigService,
    AuthenticationService,
    AuthGuard,
    LoginGuard,
    GlobalConfigService,
    MetronAlerts,
    MetronDialogBox,
    { provide: HTTP_INTERCEPTORS, useClass: DefaultHeadersInterceptor, multi: true },
    { provide: APP_INITIALIZER, useFactory: initConfig, deps: [AppConfigService], multi: true },
  ],
  bootstrap: [ AppComponent ]
})
export class AppModule {}
