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
import {async, TestBed, ComponentFixture} from '@angular/core/testing';
import {SharedModule} from '../shared.module';
import { MetronModalComponent } from './metron-modal.component';

describe('MetronModalComponent', () => {

  let fixture: ComponentFixture<MetronModalComponent>;
  let component: MetronModalComponent;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SharedModule],
      providers: [
        MetronModalComponent
      ]
    });

    fixture = TestBed.createComponent(MetronModalComponent);
    component = fixture.componentInstance;

  }));

  it('can instantiate MetronModalComponent', async(() => {
    expect(component instanceof MetronModalComponent).toBe(true);
  }));

  it('can instantiate SampleDataComponent', async(() => {
    let tmpDiv = document.createElement('div');
    let event = {
      target: {
        classList: tmpDiv.classList
      }
    };

    window.history.back = jasmine.createSpy('back');
    spyOn(component.onClose, 'emit');
    component.onModalClick(event);

    expect(window.history.back).not.toHaveBeenCalled();
    expect(component.onClose.emit).not.toHaveBeenCalled();


    tmpDiv.classList.add('dialog-pane');
    event.target.classList = tmpDiv.classList;
    component.onModalClick(event);

    expect(window.history.back).toHaveBeenCalled();
    expect(component.onClose.emit).toHaveBeenCalled();

  }));

});
