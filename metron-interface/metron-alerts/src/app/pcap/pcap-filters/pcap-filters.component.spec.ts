
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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DebugElement, SimpleChange } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { PcapFiltersComponent } from './pcap-filters.component';
import { DatePickerModule } from '../../shared/date-picker/date-picker.module';
import { PcapRequest } from '../model/pcap.request';
import { DEFAULT_TIMESTAMP_FORMAT } from '../../utils/constants';
import * as moment from 'moment/moment';

describe('PcapFiltersComponent', () => {
  let component: PcapFiltersComponent;
  let fixture: ComponentFixture<PcapFiltersComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ReactiveFormsModule,
        DatePickerModule,
      ],
      declarations: [
        PcapFiltersComponent,
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PcapFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('From date should be bound to the component', () => {
    let input = fixture.debugElement.query(By.css('[data-qe-id="start-time"]'));
    const dateString = '2020-11-11 11:11:11';
    input.componentInstance.onChange(dateString);
    fixture.detectChanges();
    expect(component.filterForm.controls.startTime.value).toBe(dateString);
  });

  it('To date should be bound to the component', () => {
    let input = fixture.debugElement.query(By.css('[data-qe-id="end-time"]'));
    const dateString = '2030-11-11 11:11:11';
    input.componentInstance.onChange(dateString);
    fixture.detectChanges();

    expect(component.filterForm.controls.endTime.value).toBe(dateString);
  });

  it('IP Source Address should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[data-qe-id="ip-src-addr"]');
    input.value = '192.168.0.1';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.filterForm.controls.ipSrcAddr.value).toBe('192.168.0.1');
  });

  it('IP Source Port should be bound to the property', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[data-qe-id="ip-src-port"]');
    input.value = '9345';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.filterForm.controls.ipSrcPort.value).toBe('9345');
  });

  it('IP Source Port should be converted to number on submit', () => {
    component.filterForm.patchValue({ ipSrcPort: '42' });
    component.search.emit = (model: PcapRequest) => {
      expect(model.ipSrcPort).toBe('42');
    };
    component.onSubmit();
  });

  it('IP Dest Address should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[data-qe-id="ip-dst-addr"]');
    input.value = '256.0.0.7';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.filterForm.controls.ipDstAddr.value).toBe('256.0.0.7');
  });

  it('IP Dest Port should be bound to the property', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[data-qe-id="ip-dst-port"]');
    input.value = '8989';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.filterForm.controls.ipDstPort.value).toBe('8989');
  });

  it('IP Dest Port should be converted to number on submit', () => {
    component.filterForm.patchValue({ ipDstPort: '42' });
    component.search.emit = (model: PcapRequest) => {
      expect(model.ipDstPort).toBe('42');
    };
    component.onSubmit();
  });

  it('Protocol should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[data-qe-id="protocol"]');
    input.value = 'TCP';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.filterForm.controls.protocol.value).toBe('TCP');
  });

  it('Include Reverse Traffic should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[data-qe-id="include-reverse"]');
    input.click();
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.filterForm.controls.includeReverse.value).toBe(true);
  });

  it('Text filter should be bound to the model', () => {
    let input: HTMLInputElement = fixture.nativeElement.querySelector('[data-qe-id="packet-filter"]');
    input.value = 'TestStringFilter';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.filterForm.controls.packetFilter.value).toBe('TestStringFilter');
  });

  it('From date should be converted to timestamp on submit', () => {
    component.filterForm.patchValue({ startTime: '2220-12-12 12:12:12' });
    component.search.emit = (model: PcapRequest) => {
      expect(model.startTimeMs).toBe(new Date(component.filterForm.controls.startTime.value).getTime());
    };
    component.onSubmit();
  });

  it('To date should be converted to timestamp on submit', () => {
    component.filterForm.patchValue({ endTimeStr: '2320-03-13 13:13:13' });
    component.search.emit = (model: PcapRequest) => {
      expect(model.endTimeMs).toBe(new Date(component.filterForm.controls.endTime.value).getTime());
    };
    component.onSubmit();
  });

  it('Port fields should be missing by default', () => {
    component.search.emit = (model: PcapRequest) => {
      expect(model.ipSrcPort).toBeFalsy();
      expect(model.ipDstPort).toBeFalsy();
    };
    component.onSubmit();
  });

  it('Filter should have an output called search', () => {
    component.search.subscribe((filterModel) => {
      expect(filterModel).toBeDefined();
    });
    component.onSubmit();
  });

  it('Filter should emit search event on submit', () => {
    spyOn(component.search, 'emit');
    component.onSubmit();
    expect(component.search.emit).toHaveBeenCalled();
  });

  it('should update request on changes', () => {
    const startTimeStr = '2220-12-12 12:12:12';
    const endTimeStr = '2320-03-13 13:13:13';

    const newModel = new PcapRequest();
    newModel.startTimeMs = new Date(startTimeStr).getTime();
    newModel.endTimeMs = new Date(endTimeStr).getTime();
    newModel.ipSrcPort = '9345';
    newModel.ipDstPort = '8989';

    component.ngOnChanges({
      model: new SimpleChange(null, newModel, false)
    });

    expect(component.filterForm.controls.startTime.value).toBe(startTimeStr);
    expect(component.filterForm.controls.endTime.value).toBe(endTimeStr);
    expect(component.filterForm.controls.ipSrcPort.value).toBe('9345');
    expect(component.filterForm.controls.ipDstPort.value).toBe('8989');
  });

  it('should update request on changes with missing port filters', () => {

    const startTimeStr = '2220-12-12 12:12:12';
    const endTimeStr = '2320-03-13 13:13:13';

    let newModel = new PcapRequest();
    newModel.startTimeMs = new Date(startTimeStr).getTime();
    newModel.endTimeMs = new Date(endTimeStr).getTime();

    component.ngOnChanges({
      model: new SimpleChange(null, newModel, false)
    });

    expect(component.filterForm.controls.startTime.value).toBe(startTimeStr);
    expect(component.filterForm.controls.endTime.value).toBe(endTimeStr);
    expect(component.filterForm.controls.ipSrcPort.value).toBe('');
    expect(component.filterForm.controls.ipDstPort.value).toBe('');
  });

  describe('Filter validation', () => {

    function setup() {
      component.queryRunning = false;
      fixture.detectChanges();
    }

    function getFieldWithSubmit(fieldId: string): { field: DebugElement, submit: DebugElement } {
      const field = fixture.debugElement.query(By.css('[data-qe-id="' + fieldId  + '"]'));
      const submit = fixture.debugElement.query(By.css('[data-qe-id="submit-button"]'));
      return {
        field,
        submit
      };
    }

    function setFieldValue(field: DebugElement, value: any) {
      field.nativeElement.value = value;
      field.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    }

    function isSubmitDisabled(submit: DebugElement): boolean {
      return submit.classes['disabled'] && submit.nativeElement.disabled;
    }

    function isFieldInvalid(field: DebugElement): boolean {
      return field.classes['ng-invalid'];
    }

    function tearDown(field: DebugElement) {
      setFieldValue(field, '');
    };

    beforeEach(setup);

    it('should disable the form if the ip source port is invalid', () => {
      const invalidValues = [
        '-42',
        '-1',
        'foobar',
        '.',
        '-',
        '+',
        'e',
        'E',
        '3.14',
        '123456',
        '65536',
        '99999',
        '2352363474576',
        '1e3',
      ];

      invalidValues.forEach((value) => {
        const els = getFieldWithSubmit('ip-src-port');
        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid without ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled without ' + value);

        setFieldValue(els.field, value);

        expect(isFieldInvalid(els.field)).toBe(true, 'the field should be invalid with ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(true, 'the submit button should be disabled with ' + value);
        tearDown(els.field);
      });
    });

    it('should keep the form enabled if the ip source port is valid', () => {
      const validValues = [
        '8080',
        '1024',
        '3000',
        '1',
        '0',
        '12345',
        '65535',
      ];

      validValues.forEach((value) => {
        const els = getFieldWithSubmit('ip-src-port');
        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid without ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled without ' + value);

        setFieldValue(els.field, value);

        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid with ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled with ' + value);
        tearDown(els.field);
      });
    });

    it('should disable the form if the ip destination port is invalid', () => {
      const invalidValues = [
        '-42',
        '-1',
        'foobar',
        '.',
        '-',
        '+',
        'e',
        'E',
        '3.14',
        '123456',
        '65536',
        '99999',
        '2352363474576',
        '1e3',
      ];

      invalidValues.forEach((value) => {
        const els = getFieldWithSubmit('ip-dst-port');
        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid without ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled without ' + value);

        setFieldValue(els.field, value);

        expect(isFieldInvalid(els.field)).toBe(true, 'the field should be invalid with ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(true, 'the submit button should be disabled with ' + value);
        tearDown(els.field);
      });
    });

    it('should keep the form enabled if the ip destination port is valid', () => {
      const validValues = [
        '8080',
        '1024',
        '3000',
        '1',
        '0',
        '12345',
        '65535',
      ];

      validValues.forEach((value) => {
        const els = getFieldWithSubmit('ip-dst-port');
        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid without ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled without ' + value);

        setFieldValue(els.field, value);

        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid with ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled with ' + value);
        tearDown(els.field);
      });
    });


    it('should disable the form if the ip source field is invalid', () => {
      const invalidValues = [
        'tst',
        0o0,
        0,
        '111.111.111',
        '222.222.222.222.222',
        '333.333.333.333',
      ];

      invalidValues.forEach((value) => {
        const els = getFieldWithSubmit('ip-src-addr');
        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid without ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled without ' + value);

        setFieldValue(els.field, value);

        expect(isFieldInvalid(els.field)).toBe(true, 'the field should be invalid with ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(true, 'the submit button should be disabled with ' + value);
        tearDown(els.field);
      });
    });

    it('should keep the form enabled if the ip source field is valid', () => {
      const validValues = [
        '0.0.0.0',
        '222.222.222.222',
        '255.255.255.255',
      ];

      validValues.forEach((value) => {
        const els = getFieldWithSubmit('ip-src-addr');
        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid without ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled without ' + value);

        setFieldValue(els.field, value);

        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid with ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled with ' + value);
        tearDown(els.field);
      });
    });

    it('start date should be valid by default', () => {
      expect(component.filterForm.get('startTime').valid).toBe(true);
    });

    it('start date is invalid if it is bigger than end date', () => {
      // start time is bigger than end time
      component.filterForm.patchValue({
        startTime: '2018-08-24 16:30:00',
        endTime: '2018-08-23 16:30:00'
      });

      expect(component.filterForm.get('startTime').valid).toBe(false);
    });

    it('start date should be valid again based on the end date', () => {
      // start time is bigger than end time
      component.filterForm.patchValue({
        startTime: '2018-08-24 16:30:00',
        endTime: '2018-08-23 16:30:00'
      });

      expect(component.filterForm.get('startTime').valid).toBe(false);

      component.filterForm.patchValue({
        endTime: '2018-08-25 16:30:00'
      });

      expect(component.filterForm.get('startTime').valid).toBe(true);
    });

    it('end date should be valid by default', () => {
      expect(component.filterForm.get('endTime').valid).toBe(true);
    });

    it('end date is invalid if it is in the future', () => {

      expect(component.filterForm.get('endTime').valid).toBe(true);

      component.filterForm.patchValue({
        endTime: moment(new Date()).add(2, 'days').format(DEFAULT_TIMESTAMP_FORMAT)
      });

      expect(component.filterForm.get('endTime').valid).toBe(false);
    });
  });
});
