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
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import {HttpClientModule, HTTP_INTERCEPTORS} from '@angular/common/http';
import { Router } from '@angular/router';
import {AppComponent} from './app.component';
import {SensorParserConfigService} from './service/sensor-parser-config.service';
import {KafkaService} from './service/kafka.service';
import {GrokValidationService} from './service/grok-validation.service';
import {StellarService} from './service/stellar.service';
import {MetronAlerts} from './shared/metron-alerts';
import {NavbarComponent} from './navbar/navbar.component';
import {VerticalNavbarComponent} from './verticalnavbar/verticalnavbar.component';
import {MetronConfigRoutingModule} from './app.routes';
import {AuthenticationService} from './service/authentication.service';
import {AuthGuard} from './shared/auth-guard';
import {LoginGuard} from './shared/login-guard';
import {SensorParserConfigModule} from './sensors/sensor-parser-config/sensor-parser-config.module';
import {SensorParserConfigReadonlyModule} from './sensors/sensor-parser-config-readonly/sensor-parser-config-readonly.module';
import {SensorParserListModule} from './sensors/sensor-parser-list/sensor-parser-list.module';
import {MetronDialogBox} from './shared/metron-dialog-box';
import {GeneralSettingsModule} from './general-settings/general-settings.module';
import {SensorEnrichmentConfigService} from './service/sensor-enrichment-config.service';
import {GlobalConfigService} from './service/global-config.service';
import {APP_CONFIG, METRON_REST_CONFIG} from './app.config';
import {StormService} from './service/storm.service';
import {SensorParserConfigHistoryService} from './service/sensor-parser-config-history.service';
import {SensorIndexingConfigService} from './service/sensor-indexing-config.service';
import {HdfsService} from './service/hdfs.service';
import { DefaultHeadersInterceptor } from './http-interceptors/default-headers.interceptor';
import { SensorAggregateModule } from './sensors/sensor-aggregate/sensor-aggregate.module';
import { SensorAggregateService } from './sensors/sensor-aggregate/sensor-aggregate.service';
import { SensorParserConfigHistoryListController } from './sensors/sensor-aggregate/sensor-parser-config-history-list.controller';
import { StoreModule } from '@ngrx/store';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { EffectsModule } from '@ngrx/effects'
import { parserConfigsReducer, groupConfigsReducer, parserStatusReducer } from './sensors/parser-configs.reducers';
import { ParserConfigEffects } from './sensors/parser-configs.effects';


@NgModule({
  imports: [ BrowserModule, FormsModule, ReactiveFormsModule, HttpClientModule, SensorParserListModule,
    SensorParserConfigModule, SensorParserConfigReadonlyModule, GeneralSettingsModule, MetronConfigRoutingModule,
    SensorAggregateModule,
    EffectsModule.forRoot([ ParserConfigEffects ]),
    StoreModule.forRoot({
      parserConfigs: parserConfigsReducer,
      groupConfigs: groupConfigsReducer,
      parserStatus: parserStatusReducer,
    }),
    StoreDevtoolsModule.instrument(),
  ],
  declarations: [ AppComponent, NavbarComponent, VerticalNavbarComponent ],
  providers: [  AuthenticationService, AuthGuard, LoginGuard, SensorParserConfigService,
    SensorParserConfigHistoryService, SensorEnrichmentConfigService, SensorIndexingConfigService,
    StormService, KafkaService, GrokValidationService, StellarService, HdfsService,
    GlobalConfigService, MetronAlerts, MetronDialogBox, { provide: APP_CONFIG, useValue: METRON_REST_CONFIG },
    { provide: HTTP_INTERCEPTORS, useClass: DefaultHeadersInterceptor, multi: true }, SensorAggregateService,
    SensorParserConfigHistoryListController],
  bootstrap:    [ AppComponent ]
})
export class AppModule {
  constructor(router: Router) {}
}
