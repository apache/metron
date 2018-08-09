
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

import { PcapFiltersComponent } from './pcap-filters.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DebugElement } from '@angular/core';
import { DatePickerModule } from '../../shared/date-picker/date-picker.module';

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

  it('Search event should contains the filter model', () => {
    spyOn(component.search, 'emit');
    component.onSubmit();
    expect(component.search.emit).toHaveBeenCalledWith(component.model);
  });

  it('Filter model structure aka PcapRequest', () => {
    expect(fixture.componentInstance.model.hasOwnProperty('startTimeMs')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('endTimeMs')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipSrcAddr')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipSrcPort')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipDstAddr')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('ipDstPort')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('protocol')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('packetFilter')).toBeTruthy();
    expect(fixture.componentInstance.model.hasOwnProperty('includeReverse')).toBeTruthy();
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
        const els = getFieldWithSubmit('ip-dest-port');
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
        const els = getFieldWithSubmit('ip-dest-port');
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
      const invalidValues = [
        '0.0.0.0',
        '222.222.222.222',
        '255.255.255.255',
      ];

      invalidValues.forEach((value) => {
        const els = getFieldWithSubmit('ip-src-addr');
        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid without ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled without ' + value);

        setFieldValue(els.field, value);

        expect(isFieldInvalid(els.field)).toBe(false, 'the field should be valid with ' + value);
        expect(isSubmitDisabled(els.submit)).toBe(false, 'the submit button should be enabled with ' + value);
        tearDown(els.field);
      });
    });

  });
});
