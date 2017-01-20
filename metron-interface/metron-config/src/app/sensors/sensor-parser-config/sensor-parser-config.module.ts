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
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {routing} from './sensor-parser-config.routing';
import {AdvancedConfigFormComponent} from '../../shared/advanced-config-form/advanced-config-form.component';
import {SampleDataComponent} from '../../shared/sample-data/sample-data.component';
import {AutocompleteComponent} from '../../shared/autocomplete/autocomplete.component';
import {SensorParserConfigComponent} from './sensor-parser-config.component';
import {SharedModule} from '../../shared/shared.module';
import {SensorStellarComponent} from '../sensor-stellar/sensor-stellar.component';
import {NumberSpinnerComponent} from '../../shared/number-spinner/number-spinner.component';
import {SensorFieldSchemaComponent} from '../sensor-field-schema/sensor-field-schema.component';
import {MultipleInputComponent} from '../../shared/multiple-input/multiple-input.component';
import {SensorGrokComponent} from '../sensor-grok/sensor-grok.component';
import {AceEditorModule} from '../../shared/ace-editor/ace-editor.module';
import {SensorThreatTriageComponent} from '../sensor-threat-triage/sensor-threat-triage.component';
import {SensorRuleEditorComponent} from '../sensor-threat-triage/rule-editor/sensor-rule-editor.component';

@NgModule ({
  imports: [ CommonModule, routing, FormsModule, ReactiveFormsModule, SharedModule, AceEditorModule ],
  declarations: [ SensorParserConfigComponent, SensorGrokComponent, SensorFieldSchemaComponent, AdvancedConfigFormComponent,
    SampleDataComponent, AutocompleteComponent, SensorStellarComponent, SensorThreatTriageComponent,
    SensorRuleEditorComponent, MultipleInputComponent, NumberSpinnerComponent ]
})
export class SensorParserConfigModule { }
