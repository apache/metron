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
import { async, TestBed, ComponentFixture } from '@angular/core/testing';
import { KafkaService } from '../../service/kafka.service';
import { Observable, throwError } from 'rxjs';
import { SampleDataComponent } from './sample-data.component';
import { SharedModule } from '../shared.module';

class MockKafkaService {
  _sample: string[];
  _sampleCounter = 0;

  public setSample(sampleMessages: string[]): void {
    this._sample = sampleMessages;
    this._sampleCounter = 0;
  }

  public sample(name: string): Observable<string> {
    if (this._sampleCounter < this._sample.length) {
      return Observable.create(observer => {
        observer.next(this._sample[this._sampleCounter++]);
        observer.complete();
      });
    }

    return throwError('Error');
  }
}

describe('SampleDataComponent', () => {
  let fixture: ComponentFixture<SampleDataComponent>;
  let sampleDataComponent: SampleDataComponent;
  let kafkaService: MockKafkaService;
  let sampleMessages: string[] = [
    'This is first message',
    'This is second message',
    'This is third message'
  ];

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SharedModule],
      declarations: [SampleDataComponent],
      providers: [
        SampleDataComponent,
        { provide: KafkaService, useClass: MockKafkaService }
      ]
    });
    fixture = TestBed.createComponent(SampleDataComponent);
    sampleDataComponent = fixture.componentInstance;
    kafkaService = TestBed.get(KafkaService);
  }));

  it('can instantiate SampleDataComponent', async(() => {
    expect(sampleDataComponent instanceof SampleDataComponent).toBe(true);
  }));

  it('should emmit messages', async(() => {
    let expectedMessage;
    let successCount = 0;
    let failureCount = 0;

    kafkaService.setSample(sampleMessages);

    sampleDataComponent.onSampleDataChanged.subscribe((message: string) => {
      ++successCount;
      expect(message).toEqual(expectedMessage);
    });

    sampleDataComponent.onSampleDataNotAvailable.subscribe(() => {
      ++failureCount;
    });

    spyOn(sampleDataComponent.onSampleDataNotAvailable, 'subscribe');
    spyOn(sampleDataComponent.onSampleDataChanged, 'subscribe');

    expectedMessage = sampleMessages[0];
    sampleDataComponent.getNextSample();
    expect(successCount).toEqual(1);
    expect(failureCount).toEqual(0);

    expectedMessage = sampleMessages[1];
    sampleDataComponent.getNextSample();
    expect(successCount).toEqual(2);
    expect(failureCount).toEqual(0);

    expectedMessage = sampleMessages[2];
    sampleDataComponent.getNextSample();
    expect(successCount).toEqual(3);
    expect(failureCount).toEqual(0);

    let tmp;
    expectedMessage = tmp;
    sampleDataComponent.getNextSample();
    expect(successCount).toEqual(3);
    expect(failureCount).toEqual(1);

    expectedMessage = sampleMessages[1];
    sampleDataComponent.getPreviousSample();
    expect(successCount).toEqual(4);
    expect(failureCount).toEqual(1);

    expectedMessage = sampleMessages[0];
    sampleDataComponent.getPreviousSample();
    expect(successCount).toEqual(5);
    expect(failureCount).toEqual(1);

    expectedMessage = sampleMessages[1];
    sampleDataComponent.getNextSample();
    expect(successCount).toEqual(6);
    expect(failureCount).toEqual(1);

    expectedMessage = sampleMessages[0];
    sampleDataComponent.getPreviousSample();
    expect(successCount).toEqual(7);
    expect(failureCount).toEqual(1);

    expectedMessage = tmp;
    sampleDataComponent.getPreviousSample();
    expect(successCount).toEqual(7);
    expect(failureCount).toEqual(1);
  }));

  it('should emmit messages on blur', async(() => {
    let expectedMessage;
    let successCount = 0;

    kafkaService.setSample(sampleMessages);

    sampleDataComponent.onSampleDataChanged.subscribe((message: string) => {
      ++successCount;
      expect(message).toEqual(expectedMessage);
    });

    expectedMessage = 'This is a simple message';
    fixture.debugElement.nativeElement.querySelector(
      'textarea'
    ).value = expectedMessage;
    sampleDataComponent.onBlur();

    expect(successCount).toEqual(1);
    expect(sampleDataComponent.sampleDataIndex).toEqual(0);
    expect(sampleDataComponent.sampleData.length).toEqual(1);
    expect(sampleDataComponent.sampleData[0]).toEqual(expectedMessage);

    expectedMessage = '';
    fixture.debugElement.nativeElement.querySelector(
      'textarea'
    ).value = expectedMessage;
    sampleDataComponent.onBlur();

    expect(successCount).toEqual(2);
    expect(sampleDataComponent.sampleDataIndex).toEqual(0);
    expect(sampleDataComponent.sampleData.length).toEqual(1);

    expectedMessage = sampleMessages[0];
    sampleDataComponent.getNextSample();

    expect(successCount).toEqual(3);
    expect(sampleDataComponent.sampleDataIndex).toEqual(1);
    expect(sampleDataComponent.sampleData.length).toEqual(2);
    expect(sampleDataComponent.sampleData[1]).toEqual(sampleMessages[0]);
  }));
});
