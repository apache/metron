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
import {ReactiveFormsModule} from '@angular/forms';
import {routing} from './sensor-parser-config.routing';
import {SensorParserConfigComponent} from './sensor-parser-config.component';
import {SharedModule} from '../../shared/shared.module';
import {NumberSpinnerModule} from '../../shared/number-spinner/number-spinner.module';
import {AdvancedConfigFormModule} from '../../shared/advanced-config-form/advanced-config-form.module';
import {SensorGrokModule} from '../sensor-grok/sensor-grok.module';
import {SensorFieldSchemaModule} from '../sensor-field-schema/sensor-field-schema.module';
import {SensorRawJsonModule} from '../sensor-raw-json/sensor-raw-json.module';
import {SensorThreatTriageModule} from '../sensor-threat-triage/sensor-threat-triage.module';
import {SensorStormSettingsModule} from '../sensor-storm-settings/sensor-storm-settings.module';

@NgModule ({
  imports: [ routing, ReactiveFormsModule, SharedModule, NumberSpinnerModule, AdvancedConfigFormModule,
                SensorGrokModule, SensorFieldSchemaModule, SensorRawJsonModule, SensorThreatTriageModule, SensorStormSettingsModule ],
  declarations: [ SensorParserConfigComponent ]
})
export class SensorParserConfigModule { }
