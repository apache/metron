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
import { NgModule }            from '@angular/core';
import { CommonModule }        from '@angular/common';
import { FormsModule }         from '@angular/forms';
import { AlertSeverityDirective } from './directives/alert-severity.directive';
import { MetronTableDirective } from './metron-table/metron-table.directive';
import { NavContentDirective } from './directives/nav-content.directive';
import { CenterEllipsesPipe } from './pipes/center-ellipses.pipe';
import { AlertSearchDirective } from './directives/alert-search.directive';
import { ColumnNameTranslatePipe } from './pipes/column-name-translate.pipe';
import { MapKeysPipe } from './pipes/map-keys.pipe';
import { AlertSeverityHexagonDirective } from './directives/alert-severity-hexagon.directive';
import { TimeLapsePipe } from './pipes/time-lapse.pipe';

@NgModule({
  imports:  [
    CommonModule
  ],
  declarations: [
    AlertSeverityDirective,
    MetronTableDirective,
    NavContentDirective,
    CenterEllipsesPipe,
    AlertSearchDirective,
    ColumnNameTranslatePipe,
    AlertSeverityHexagonDirective,
    TimeLapsePipe,
    MapKeysPipe,
  ],
  exports:  [
    CommonModule,
    FormsModule,
    AlertSeverityDirective,
    MetronTableDirective,
    NavContentDirective,
    CenterEllipsesPipe,
    AlertSearchDirective,
    ColumnNameTranslatePipe,
    AlertSeverityHexagonDirective,
    TimeLapsePipe,
    MapKeysPipe,
  ]
})
export class SharedModule { }
