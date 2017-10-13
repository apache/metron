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
import {HttpModule} from '@angular/http';
import {AppComponent} from './app.component';
import {SensorParserConfigService} from './service/sensor-parser-config.service';
import {KafkaService} from './service/kafka.service';
import {GrokValidationService} from './service/grok-validation.service';
import {StellarService} from './service/stellar.service';
import {MetronAlerts} from './shared/metron-alerts';
import {NavbarComponent} from './navbar/navbar.component';
import {VerticalNavbarComponent} from './verticalnavbar/verticalnavbar.component';
import {routing, appRoutingProviders} from './app.routes';
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


@NgModule({
  imports: [ BrowserModule, routing, FormsModule, ReactiveFormsModule, HttpModule, SensorParserListModule,
    SensorParserConfigModule, SensorParserConfigReadonlyModule, GeneralSettingsModule ],
  declarations: [ AppComponent, NavbarComponent, VerticalNavbarComponent ],
  providers: [  AuthenticationService, AuthGuard, LoginGuard, SensorParserConfigService,
    SensorParserConfigHistoryService, SensorEnrichmentConfigService, SensorIndexingConfigService,
    StormService, KafkaService, GrokValidationService, StellarService, HdfsService,
    GlobalConfigService, MetronAlerts, MetronDialogBox, appRoutingProviders, { provide: APP_CONFIG, useValue: METRON_REST_CONFIG }],
  bootstrap:    [ AppComponent ]
})
export class AppModule { }
