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
import { NgModule } from '@angular/core';
import { SensorParserListModule } from './sensor-parser-list/sensor-parser-list.module';
import { SensorParserConfigReadonlyModule } from './sensor-parser-config-readonly/sensor-parser-config-readonly.module';
import { SensorParserConfigModule } from './sensor-parser-config/sensor-parser-config.module';
import { SensorAggregateModule } from './sensor-aggregate/sensor-aggregate.module';
import { SensorParserConfigHistoryListController } from './sensor-aggregate/sensor-parser-config-history-list.controller';
import { GrokValidationService } from '../service/grok-validation.service';
import { StellarService } from '../service/stellar.service';
import { HdfsService } from '../service/hdfs.service';
import { SensorAggregateService } from './sensor-aggregate/sensor-aggregate.service';
import { SensorIndexingConfigService } from '../service/sensor-indexing-config.service';
import { SensorEnrichmentConfigService } from '../service/sensor-enrichment-config.service';
import { SensorParserConfigHistoryService } from '../service/sensor-parser-config-history.service';
import { SensorParserConfigService } from '../service/sensor-parser-config.service';
import { KafkaService } from '../service/kafka.service';
import { StormService } from '../service/storm.service';
import { EffectsModule } from '@ngrx/effects';
import { ParserConfigEffects } from './parser-configs.effects';
import { StoreModule } from '@ngrx/store';
import { reducers } from './reducers';

@NgModule ({
  imports: [
    SensorParserListModule,
    SensorParserConfigReadonlyModule,
    SensorParserConfigModule,
    SensorAggregateModule,
    EffectsModule.forFeature([ ParserConfigEffects ]),
    StoreModule.forFeature('sensors', reducers),
  ],
  declarations: [],
  providers: [
    SensorParserConfigService,
    SensorParserConfigHistoryService, SensorEnrichmentConfigService, SensorIndexingConfigService,
    StormService, KafkaService, GrokValidationService, StellarService, HdfsService, SensorAggregateService,
    SensorParserConfigHistoryListController
  ],
})
export class SensorsModule { }
