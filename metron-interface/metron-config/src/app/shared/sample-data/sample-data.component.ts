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
import {Component, Input, Output, EventEmitter, ViewChild, ElementRef} from '@angular/core';
import {KafkaService} from '../../service/kafka.service';

@Component({
  selector: 'metron-config-sample-data',
  templateUrl: 'sample-data.component.html',
  styleUrls: ['sample-data.component.scss'],
})

export class SampleDataComponent {

  @Input() topic: string;
  @Output() onSampleDataChanged = new EventEmitter<string>();
  @Output() onSampleDataNotAvailable = new EventEmitter<void>();

  @ViewChild('sampleDataElement') sampleDataElement: ElementRef;

  sampleData: string[] = [];
  sampleDataIndex: number = -1;
  placeHolderText = 'Paste Sample Message' + '\n' +
                    'A data sample cannot automatically be loaded. Connect to a Kafka Topic or paste a message here.';


  constructor(private kafkaService: KafkaService) {
  }


  getPreviousSample() {
    if (this.sampleDataIndex > 0) {
      this.sampleDataIndex = this.sampleDataIndex - 1;
      this.onSampleDataChanged.emit(this.sampleData[this.sampleDataIndex]);
    }
  }

  getNextSample() {
    if (this.sampleData.length - 1 === this.sampleDataIndex) {
      this.kafkaService.sample(this.topic).subscribe((sampleData: string) => {
          this.sampleDataIndex = this.sampleDataIndex + 1;
          this.sampleData[this.sampleDataIndex] = sampleData;
          this.onSampleDataChanged.emit(this.sampleData[this.sampleDataIndex]);
        },
        error => {
          this.onSampleDataNotAvailable.emit();
        });
    } else {
      this.sampleDataIndex = this.sampleDataIndex + 1;
      this.onSampleDataChanged.emit(this.sampleData[this.sampleDataIndex]);
    }

  }

  onBlur() {
    let currentValue = this.sampleDataElement.nativeElement.value;

    if (currentValue.trim() !== '' && this.sampleData[this.sampleDataIndex] !== currentValue) {
      this.sampleDataIndex = this.sampleDataIndex + 1;
      this.sampleData[this.sampleDataIndex] = currentValue;
      this.onSampleDataChanged.emit(this.sampleData[this.sampleDataIndex]);
    }

    if (currentValue.trim() === '') {
      this.sampleDataElement.nativeElement.placeholder = this.placeHolderText;
      this.onSampleDataChanged.emit('');
    }
  }

}
