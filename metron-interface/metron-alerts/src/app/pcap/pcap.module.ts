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
import { CommonModule } from '@angular/common';  
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { routing } from './pcap.routing';

import { PcapListComponent } from './pcap-list/pcap-list.component';
import { PcapPacketComponent } from './pcap-packet/pcap-packet.component';
import { PcapFiltersComponent } from './pcap-filters/pcap-filters.component';
import { PcapPanelComponent } from './pcap-panel/pcap-panel.component';
import { PcapPacketLineComponent } from './pcap-packet-line/pcap-packet-line.component';

import { PcapService } from './service/pcap.service'
 
@NgModule({
  imports: [
    routing,
    CommonModule,
    FormsModule,
    HttpModule    
  ],
  declarations: [
    PcapListComponent,
    PcapPacketComponent,
    PcapFiltersComponent,
    PcapPanelComponent,
    PcapPacketLineComponent
  ],
  exports: [ PcapPanelComponent ],
  providers: [ PcapService ]
})
export class PcapModule {}
