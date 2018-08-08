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
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormControl, Validators, ValidationErrors } from '@angular/forms';

import * as moment from 'moment/moment';
import { DEFAULT_TIMESTAMP_FORMAT } from '../../utils/constants';

import { PcapRequest } from '../model/pcap.request';

const endTime = new Date();
const startTime = new Date().setDate(endTime.getDate() - 5);

function dateRangeValidator(filterForm: FormGroup): ValidationErrors | null {
  const startTimeMs = new Date(filterForm.controls.startTimeMs.value).getTime();
  const endTimeMs = new Date(filterForm.controls.endTimeMs.value).getTime();

  if (startTimeMs > endTimeMs || endTimeMs > new Date().getTime()) {
    return { error: 'Selected date range is invalid.' };
  }
  return null;
}

@Component({
  selector: 'app-pcap-filters',
  templateUrl: './pcap-filters.component.html',
  styleUrls: ['./pcap-filters.component.scss']
})
export class PcapFiltersComponent {

  @Input() queryRunning = true;
  @Output() search: EventEmitter<PcapRequest> = new EventEmitter<PcapRequest>();

  private validIp: RegExp = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}$/;
  private validPort: RegExp = /^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$/;

  filterForm = new FormGroup({
    startTimeMs: new FormControl(moment(startTime).format(DEFAULT_TIMESTAMP_FORMAT)),
    endTimeMs: new FormControl(moment(endTime).format(DEFAULT_TIMESTAMP_FORMAT)),
    ipSrcAddr: new FormControl('', Validators.pattern(this.validIp)),
    ipSrcPort: new FormControl('', Validators.pattern(this.validPort)),
    ipDstAddr: new FormControl('', Validators.pattern(this.validIp)),
    ipDstPort: new FormControl('', Validators.pattern(this.validPort)),
    protocol: new FormControl(),
    includeReverse: new FormControl(),
    packetFilter: new FormControl(),
  }, dateRangeValidator);

  model = new PcapRequest();

  onSubmit() {
    this.model = this.filterForm.value;
    this.model.startTimeMs = new Date(this.filterForm.value.startTimeMs).getTime();
    this.model.endTimeMs = new Date(this.filterForm.value.endTimeMs).getTime();
    this.model.ipSrcPort = +this.filterForm.value.ipSrcPort;
    this.model.ipDstPort = +this.filterForm.value.ipDstPort;

    this.search.emit(this.model);
  }
}
