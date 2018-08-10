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
import {Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges} from '@angular/core';
import * as moment from 'moment/moment';
import { DEFAULT_TIMESTAMP_FORMAT } from '../../utils/constants';

import { PcapRequest } from '../model/pcap.request';

@Component({
  selector: 'app-pcap-filters',
  templateUrl: './pcap-filters.component.html',
  styleUrls: ['./pcap-filters.component.scss']
})
export class PcapFiltersComponent implements OnInit, OnChanges {

  @Input() queryRunning: boolean = true;
  @Input() model: PcapRequest = new PcapRequest();
  @Output() search: EventEmitter<PcapRequest> = new EventEmitter<PcapRequest>();

  startTimeStr: string;
  endTimeStr: string;
  ipSrcPort: string = '';
  ipDstPort: string = '';

  constructor() { }

  ngOnInit() {
    const endTime = new Date();
    const startTime = new Date().setDate(endTime.getDate() - 5);

    this.startTimeStr = moment(startTime).format(DEFAULT_TIMESTAMP_FORMAT);
    this.endTimeStr = moment(endTime).format(DEFAULT_TIMESTAMP_FORMAT);
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
    this.model.startTimeMs = new Date(this.startTimeStr).getTime();
    this.model.endTimeMs = new Date(this.endTimeStr).getTime();
    if (this.ipSrcPort !== '') {
      this.model.ipSrcPort = +this.ipSrcPort;
    } else {
      delete this.model.ipSrcPort;
    }
    if (this.ipDstPort !== '') {
      this.model.ipDstPort = +this.ipDstPort;
    } else {
      delete this.model.ipDstPort;
    }

    this.search.emit(this.model);
  }
}
