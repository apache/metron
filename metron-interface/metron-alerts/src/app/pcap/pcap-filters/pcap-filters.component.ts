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
import {Component, Input, Output, EventEmitter, OnChanges, SimpleChanges} from '@angular/core';
import { FormGroup, FormControl, Validators, ValidationErrors } from '@angular/forms';

import * as moment from 'moment/moment';
import { DEFAULT_TIMESTAMP_FORMAT } from '../../utils/constants';

import { PcapRequest } from '../model/pcap.request';

const endTime = new Date();
const startTime = new Date().setDate(endTime.getDate() - 5);

function dateRangeValidator(formControl: FormControl): ValidationErrors | null {
  if (!formControl.parent) {
    return null;
  }

  const filterForm = formControl.parent;
  const startTimeMs = new Date(filterForm.controls['startTime'].value).getTime();
  const endTimeMs = new Date(filterForm.controls['endTime'].value).getTime();

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
export class PcapFiltersComponent implements OnChanges {

  @Input() queryRunning = true;
  @Input() model: PcapRequest = new PcapRequest();
  @Output() search: EventEmitter<PcapRequest> = new EventEmitter<PcapRequest>();

  private validIp: RegExp = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}$/;
  private validPort: RegExp = /^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$/;

  filterForm = new FormGroup({
    startTime: new FormControl(moment(startTime).format(DEFAULT_TIMESTAMP_FORMAT), dateRangeValidator),
    endTime: new FormControl(moment(endTime).format(DEFAULT_TIMESTAMP_FORMAT), dateRangeValidator),
    ipSrcAddr: new FormControl('', Validators.pattern(this.validIp)),
    ipSrcPort: new FormControl('', Validators.pattern(this.validPort)),
    ipDstAddr: new FormControl('', Validators.pattern(this.validIp)),
    ipDstPort: new FormControl('', Validators.pattern(this.validPort)),
    protocol: new FormControl(),
    includeReverse: new FormControl(),
    packetFilter: new FormControl(),
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['model']) {
      const newModel: PcapRequest = Object.assign(this.model, changes['model'].currentValue);

      this.filterForm.patchValue({ startTime: moment(newModel.startTimeMs).format(DEFAULT_TIMESTAMP_FORMAT) });
      this.filterForm.patchValue({ endTime: moment(newModel.endTimeMs).format(DEFAULT_TIMESTAMP_FORMAT) });
      this.filterForm.patchValue({ ipSrcAddr: newModel.ipSrcAddr });
      this.filterForm.patchValue({ ipSrcPort: newModel.ipSrcPort });
      this.filterForm.patchValue({ ipDstAddr: newModel.ipDstAddr });
      this.filterForm.patchValue({ ipDstPort: newModel.ipDstPort });
      this.filterForm.patchValue({ protocol: newModel.protocol });
      this.filterForm.patchValue({ includeReverse: newModel.includeReverse });
      this.filterForm.patchValue({ packetFilter: newModel.packetFilter });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['model']) {
      this.startTimeStr = moment(changes['model'].currentValue.startTimeMs).format(DEFAULT_TIMESTAMP_FORMAT);
      this.endTimeStr = moment(changes['model'].currentValue.endTimeMs).format(DEFAULT_TIMESTAMP_FORMAT);
      let newIpSrcPort = changes['model'].currentValue.ipSrcPort;
      this.ipSrcPort = newIpSrcPort ? newIpSrcPort.toString() : '';
      let newIpDstPort = changes['model'].currentValue.ipDstPort;
      this.ipDstPort = newIpDstPort ? newIpDstPort.toString() : '';
    }
  }

  onSubmit() {
    this.model = this.filterForm.value;

    this.model.startTimeMs = new Date(this.filterForm.value.startTime).getTime();
    this.model.endTimeMs = new Date(this.filterForm.value.endTime).getTime();

    if (this.filterForm.value.ipSrcPort !== '') {
      this.model.ipSrcPort = +this.filterForm.value.ipSrcPort;
    }
    if (this.filterForm.value.ipDstPort !== '') {
      this.model.ipDstPort = +this.filterForm.value.ipDstPort;
    }

    this.search.emit(this.model);
  }
}
