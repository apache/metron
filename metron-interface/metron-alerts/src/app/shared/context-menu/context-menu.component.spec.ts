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
import { ComponentFixture, TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ContextMenuComponent } from './context-menu.component';
import { ContextMenuService } from './context-menu.service';
import { Component, Injectable } from '@angular/core';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

const FAKE_CONFIG_SVC_URL = '/test/config/menu/url';

@Injectable()
class FakeContextMenuService {

  fakeConfig = {}

  getConfig() {
    return of(this.fakeConfig);
  }
}

@Component({
  template: `
    <div ctxMenu
      ctxMenuId="testMenuConfigId"
      ctxMenuTitle="This is a test"
      [ctxMenuItems]="[
        { label: 'Test Label 01', event: 'customEventOne'},
        { label: 'Test Label 02', event: 'customEventTwo'}
      ]"
      [ctxMenuData]="{
        testMenuConfigId: 'testValue',
        customKey: 'customValue'
      }">
      Context Menu Test In Progress...
    </div>
  `
})
class TestComponent {}

describe('ContextMenuComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveHostEl: any;

  let fakeContextMenuSvc: FakeContextMenuService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      declarations: [ ContextMenuComponent, TestComponent ],
      providers: [
        { provide: ContextMenuService, useClass: FakeContextMenuService }
      ]
    })
    .compileComponents();

    fakeContextMenuSvc = getTestBed().get(ContextMenuService);
    fixture = TestBed.createComponent(TestComponent);
    directiveHostEl = fixture.debugElement.query(By.directive(ContextMenuComponent)).nativeElement;
  });

  afterEach(() => {
    fixture.destroy();
  })

  it('should create', () => {
    expect(fixture).toBeTruthy();
  });

  it('should show context menu on left click when feature enabled', () => {
    fakeContextMenuSvc.fakeConfig = {
        isEnabled: true,
        config: {
          menuKey: []
        }
      };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    expect(document.body.querySelector('[data-qe-id="cm-dropdown"]')).toBeTruthy();
  });

  it('should NOT show context menu on left click when feature IS NOT enabled', () => {
    fakeContextMenuSvc.fakeConfig = {
      isEnabled: false,
      config: {
        menuKey: []
      }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    expect(document.body.querySelector('[data-qe-id="cm-dropdown"]')).toBeFalsy();
  });

  it('should close context menu if user clicks outside of it', () => {
    fakeContextMenuSvc.fakeConfig = {
      isEnabled: true,
      config: {
        menuKey: []
      }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    expect(document.body.querySelector('[data-qe-id="cm-dropdown"]')).toBeTruthy();

    (document.body.querySelector('[data-qe-id="cm-outside"]') as HTMLElement).click();
    fixture.detectChanges();

    expect(document.body.querySelector('.dropdown-menu')).toBeFalsy();
  });

  it('should render predefined menu items', () => {
    fakeContextMenuSvc.fakeConfig = {
      isEnabled: true,
      config: {
        menuKey: []
      }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    expect(document.body.querySelector('[data-qe-id="cm-predefined-item"]')).toBeTruthy();
  });

  it('should render multiple predefined menu items', () => {
    fakeContextMenuSvc.fakeConfig = {
      isEnabled: true,
      config: {
        menuKey: []
      }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    expect(document.body.querySelectorAll('[data-qe-id="cm-predefined-item"]').length).toBe(2);
  });

  it('predefined menu item should render label', () => {
    fakeContextMenuSvc.fakeConfig = {
      isEnabled: true,
      config: {
        menuKey: []
      }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    expect(document.body
      .querySelector('[data-qe-id="cm-predefined-item"]')
      .firstChild.textContent
    ).toBe('Test Label 01');
  });

  it('should fetch dymamic menu items', () => {
    spyOn(fakeContextMenuSvc, 'getConfig').and.callThrough();
    fixture.detectChanges();

    expect(fakeContextMenuSvc.getConfig).toHaveBeenCalled();
  });

  it('should render dymamic menu items', () => {
    fakeContextMenuSvc.fakeConfig = {
      isEnabled: true,
      config: { testMenuConfigId: [
        { label: 'dynamic test item #4532', urlPattern: '/myTestUri/{}' },
        { label: 'dynamic test item #756', urlPattern: '/myTestUri/{}' },
      ] }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    expect(document.body
      .querySelectorAll('[data-qe-id="cm-dynamic-item"]')[0]
      .firstChild.textContent
    ).toBe('dynamic test item #4532');

    expect(document.body
      .querySelectorAll('[data-qe-id="cm-dynamic-item"]')[1]
      .firstChild.textContent
    ).toBe('dynamic test item #756');
  });

  it('should emit the configured event if user clicks on predefined menu item', () => {
    fakeContextMenuSvc.fakeConfig = {
        isEnabled: true,
        config: {}
      };
    fixture.detectChanges();

    directiveHostEl.addEventListener('customEventOne', (event) => {
      expect(event.type).toBe('customEventOne');
    });

    directiveHostEl.click();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-qe-id="cm-predefined-item"]').click()
    fixture.detectChanges();
  });

  it('should call window.open if user clicks on dynamic menu item', () => {
    const RAW_URL = '/myTestUri/{}';
    const EXPECTED_URL = '/myTestUri/testValue';
    const DYNAMIC_ITEM = '[data-qe-id="cm-dynamic-item"]';

    spyOn(window, 'open');

    fakeContextMenuSvc.fakeConfig = {
      isEnabled: true,
      config: {
        testMenuConfigId: [{ label: 'dynamic test item #98', urlPattern: RAW_URL }]
      }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    fixture.nativeElement.querySelector(DYNAMIC_ITEM).click()
    fixture.detectChanges();

    expect(window.open).toHaveBeenCalledWith(EXPECTED_URL);
  });

  it('urlPatter should be parsed and resolved when calling window.open', () => {
    const RAW_URL = '/myTestUri/{}/customkeyshouldresolveto/{customKey}';
    const EXPECTED_URL = '/myTestUri/testValue/customkeyshouldresolveto/customValue';
    const DYNAMIC_ITEM = '[data-qe-id="cm-dynamic-item"]';

    spyOn(window, 'open');

    fakeContextMenuSvc.fakeConfig = {
      isEnabled: true,
      config: {
        testMenuConfigId: [{ label: 'dynamic test item #98', urlPattern: RAW_URL }]
      }
    };
    fixture.detectChanges();

    directiveHostEl.click();
    fixture.detectChanges();

    fixture.nativeElement.querySelector(DYNAMIC_ITEM).click()
    fixture.detectChanges();

    expect(window.open).toHaveBeenCalledWith(EXPECTED_URL);
  });

});
