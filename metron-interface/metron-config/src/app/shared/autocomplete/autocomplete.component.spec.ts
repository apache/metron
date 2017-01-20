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
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {AutocompleteComponent} from './autocomplete.component';
import {AutocompleteTransformationsStatement} from './autocomplete-transformations-statement';
import {AutocompleteGrokStatement} from './autocomplete-grok-statement';
import {AutocompleteOption} from '../../model/autocomplete-option';
declare let $: any;

describe('Component: Autocomplete', () => {

  let comp: AutocompleteComponent;
  let fixture: ComponentFixture<AutocompleteComponent>;
  let option1 = new AutocompleteOption();
  option1.name = 'FUNCTION_1';
  option1.description = 'description_1';
  option1.title = 'title_1';
  option1.isFunction = true;
  let option2 = new AutocompleteOption();
  option2.name = 'FUNCTION_2';
  option2.description = 'description_2';
  option2.title = 'title_2';
  option2.isFunction = true;
  let option3 = new AutocompleteOption();
  option3.name = 'SOME_FUNCTION_3';
  option3.description = 'description_3';
  option3.title = 'title_3';
  option3.isFunction = true;
  let field1 = new AutocompleteOption();
  field1.name = 'field_1';
  field1.isFunction = false;
  let allOptions = [option1, option2, option3, field1];

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [AutocompleteComponent]

    }).compileComponents()
      .then(() => {
        fixture = TestBed.createComponent(AutocompleteComponent);
        comp = fixture.componentInstance;
      });

  }));

  it('should create a instance', async(() => {

    let component: AutocompleteComponent = fixture.componentInstance;
    expect(component).toBeTruthy();
  }));

  it('should canProcessKeyup', async(() => {
    let component: AutocompleteComponent = fixture.componentInstance;

    fixture.debugElement.nativeElement.querySelector('.autocomplete').innerHTML = 'SQUID %{key:value} %{key2:value2}';

    let autoCompleteElement = document.getElementsByClassName('autocomplete').item(0);
    let selection = window.getSelection();
    let range = document.createRange();
    range.setStart(autoCompleteElement.firstChild, 0);
    range.setEnd(autoCompleteElement.firstChild, 4);
    selection.removeAllRanges();
    selection.addRange(range);

    expect(component.canProcessKeyup()).toEqual(false);

    range.setEnd(autoCompleteElement.firstChild, 0);
    selection.removeAllRanges();
    selection.addRange(range);

    expect(component.canProcessKeyup()).toEqual(true);

  }));

  it('should storeCaretPosition', async(() => {
    let component: AutocompleteComponent = fixture.componentInstance;

    component.caretPosition = 10;
    window.getSelection().removeAllRanges();
    component.storeCaretPosition();
    expect(component.caretPosition).toEqual(0);

    component.caretPosition = 0;
    fixture.debugElement.nativeElement.querySelector('.autocomplete').textContent = 'SQUID %{key:value} %{key2:value2}';
    component.setCaret(19);
    component.storeCaretPosition();
    expect(component.caretPosition).toEqual(19);

    fixture.debugElement.nativeElement.querySelector('.autocomplete').textContent = 'THIS IS SQUID STATEMENT TO TEST';
    component.setCaret(22);
    component.storeCaretPosition();
    expect(component.caretPosition).toEqual(22);

  }));

  it('should getCaretCoordinates', async(() => {
    let component: AutocompleteComponent = fixture.componentInstance;

    fixture.debugElement.nativeElement.querySelector('.autocomplete').innerHTML = 'SQUID %{key:value} %{key2:value2}';

    let autoCompleteElement = document.getElementsByClassName('autocomplete').item(0);
    let selection = window.getSelection();
    let range = document.createRange();

    expect(component.getCaretCoordinates()).toEqual({x: 0, y: 0});

    range.setStart(autoCompleteElement.firstChild, 4);
    range.setEnd(autoCompleteElement.firstChild, 5);
    selection.removeAllRanges();
    selection.addRange(range);
    expect(component.getCaretCoordinates()).toEqual({x: 39, y: 9});

    range.setStart(autoCompleteElement.firstChild, 0);
    selection.removeAllRanges();
    selection.addRange(range);
    expect(component.getCaretCoordinates()).toEqual({x: 9, y: 9});

    selection.removeAllRanges();
    expect(component.getCaretCoordinates()).toEqual({x: 0, y: 0});

  }));

  it('should setAutocompleteDropdownPosition', async(() => {
    let component: AutocompleteComponent = fixture.componentInstance;

    fixture.debugElement.nativeElement.querySelector('.autocomplete').innerHTML = 'SQUID %{key:value} %{key2:value2}';

    let autoCompleteElement = document.getElementsByClassName('autocomplete').item(0);
    let selection = window.getSelection();
    let range = document.createRange();
    range.setStart(autoCompleteElement.firstChild, 4);
    range.setEnd(autoCompleteElement.firstChild, 4);
    selection.removeAllRanges();
    selection.addRange(range);

    component.setAutocompleteDropdownPosition();
    let select = fixture.debugElement.nativeElement.getElementsByClassName('autocomplete-select')[0];
    expect(select.style.left).toEqual('9px');
    expect(select.style.top).toEqual('21px');

    range.setStart(autoCompleteElement.firstChild, 5);
    range.setEnd(autoCompleteElement.firstChild, 5);
    selection.removeAllRanges();
    selection.addRange(range);
    component.setAutocompleteDropdownPosition();
    expect(select.style.left).toEqual('19px');
    expect(select.style.top).toEqual('21px');

    component.showAutoComplete = true;
    selection.removeAllRanges();
    component.setAutocompleteDropdownPosition();
    expect(select.style.left).toEqual('19px');
    expect(select.style.top).toEqual('21px');
    expect(component.showAutoComplete).toEqual(false);

  }));

  it('should emit statement', async(() => {
    let component: AutocompleteComponent = fixture.componentInstance;

    spyOn(component.result, 'emit');
    component.statement = 'SQUID %{abc:xyz} ';
    component.emitResult();

    expect(component.result.emit).toHaveBeenCalledWith('SQUID %{abc:xyz}');

  }));

  it('should appendToStatement', async(() => {
    let component: AutocompleteComponent = fixture.componentInstance;


    component.autocompleteStatementGenerator = new AutocompleteGrokStatement();
    component.statement = 'SQUID %{key:value} %{key2:value2}';
    fixture.debugElement.nativeElement.querySelector('.autocomplete').textContent = 'SQUID %{key:value} %{key2:value2}';
    component.showAutoComplete = true;
    component.caretPosition = 11;

    let option = new AutocompleteOption();
    option.name = 'lm';
    component.appendToStatement(option);

    expect(component.statement).toEqual('SQUID %{lm:value} %{key2:value2}');
    expect(component.showAutoComplete).toEqual(false);

    component.statement = '';
    component.caretPosition = 0;
    option.name = 'xyz';
    component.appendToStatement(option);
    expect(component.statement).toEqual('%{xyz:}');

    component.statement = '%{';
    component.caretPosition = 2;
    option.name = 'xyz';
    component.appendToStatement(option);
    expect(component.statement).toEqual('%{xyz:}');

    component.statement = '%{xyz:}';
    component.caretPosition = 7;
    option.name = 'abc';
    component.appendToStatement(option);
    expect(component.statement).toEqual('%{xyz:}%{abc:}');

    component.statement = '%{xyz:} ab';
    component.caretPosition = 10;
    component.appendToStatement(option);
    expect(component.statement).toEqual('%{xyz:} %{abc:}');

  }));

  it('should removeUserTypedString', async(() => {
    let component: AutocompleteComponent = fixture.componentInstance;

    expect(component.removeUserTypedString('SQUID %{key')).toEqual('SQUID %{');
    expect(component.removeUserTypedString('SQUID %{key:value')).toEqual('SQUID %{key:');

  }));

  it('should process onKeyDown', async(() => {
    this.createEvent = function (keycode) {
      let event = {
        stopPropagation: function () {
        },
        preventDefault: function () {
        },
        stopImmediatePropagation: function () {
        },
        keyCode: keycode
      };

      spyOn(event, 'stopPropagation');
      spyOn(event, 'preventDefault');
      spyOn(event, 'stopImmediatePropagation');

      return event;
    };

    let component: AutocompleteComponent = fixture.componentInstance;
    component.showAutoComplete = true;

    event = this.createEvent(13);
    expect(component.onKeyDown(event)).toEqual(false);
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(event.preventDefault).not.toHaveBeenCalled();
    expect(event.stopImmediatePropagation).not.toHaveBeenCalled();

    event = this.createEvent(40);
    expect(component.onKeyDown(event)).toEqual(false);
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(event.preventDefault).toHaveBeenCalled();
    expect(event.stopImmediatePropagation).toHaveBeenCalled();

    event = this.createEvent(38);
    expect(component.onKeyDown(event)).toEqual(false);
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(event.preventDefault).toHaveBeenCalled();
    expect(event.stopImmediatePropagation).toHaveBeenCalled();

    event = this.createEvent(22);
    expect(component.onKeyDown(event)).toEqual(undefined);
    expect(event.stopPropagation).not.toHaveBeenCalled();
    expect(event.preventDefault).not.toHaveBeenCalled();
    expect(event.stopImmediatePropagation).not.toHaveBeenCalled();

  }));

  it('should process onKeyUp', async(() => {
    this.createEvent = function (keycode) {
      let event = {
        stopPropagation: function () {
        },
        preventDefault: function () {
        },
        stopImmediatePropagation: function () {
        },
        keyCode: keycode
      };

      spyOn(event, 'stopPropagation');
      spyOn(event, 'preventDefault');
      spyOn(event, 'stopImmediatePropagation');

      return event;
    };

    let component: AutocompleteComponent = fixture.componentInstance;
    component.showAutoComplete = true;
    component.statement = 'F';
    component.options = allOptions;
    component.autocompleteStatementGenerator = new AutocompleteTransformationsStatement();

    fixture.detectChanges();

    fixture.debugElement.nativeElement.querySelector('.autocomplete').textContent = 'F';
    let autoCompleteElement = fixture.debugElement.nativeElement.getElementsByClassName('autocomplete').item(0);
    let selection = window.getSelection();
    let range = document.createRange();
    range.setStart(autoCompleteElement.firstChild, 1);
    range.setEnd(autoCompleteElement.firstChild, 1);
    selection.removeAllRanges();
    selection.addRange(range);

    spyOn(component, 'emitResult').and.callThrough();
    spyOn(component, 'storeCaretPosition').and.callThrough();
    spyOn(component, 'setAutocompleteDropdownPosition').and.callThrough();
    spyOn(component, 'bringItemToViewport').and.callThrough();
    let showPopoverSpy = spyOn(component, 'showPopover');
    let hidePopoverSpy = spyOn(component, 'hidePopover');

    event = this.createEvent(17);
    expect(component.onKeyup(event)).toEqual(false);
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(event.preventDefault).toHaveBeenCalled();
    expect(event.stopImmediatePropagation).toHaveBeenCalled();
    expect(component.emitResult).toHaveBeenCalled();
    expect(component.storeCaretPosition).toHaveBeenCalled();
    expect(component.setAutocompleteDropdownPosition).toHaveBeenCalled();

    // Press Shift+F
    event = this.createEvent(82);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.stringTillCaret).toEqual('F');
    expect(component.filteredOptions).toEqual([option1, option2]);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).not.toHaveBeenCalled();

    // Press Down Arrow
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(40);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.stringTillCaret).toEqual('F');
    expect(component.filteredOptions).toEqual([option1, option2]);
    expect(component.filteredOptions[0].isActive).toEqual(false);
    expect(component.filteredOptions[1].isActive).toEqual(true);
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[1]);
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[0]);
    expect(component.bringItemToViewport).toHaveBeenCalledWith(component.filteredOptions[1]);
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.filteredOptions[1].isActive).toEqual(false);
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[0]);
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[1]);
    expect(component.bringItemToViewport).toHaveBeenCalledWith(component.filteredOptions[0]);

    // Press Up Arrow
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(38);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.caretPosition).toEqual(1);
    expect(component.stringTillCaret).toEqual('F');
    expect(component.filteredOptions).toEqual([option1, option2]);
    expect(component.filteredOptions[0].isActive).toEqual(false);
    expect(component.filteredOptions[1].isActive).toEqual(true);
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[1]);
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[0]);
    expect(component.bringItemToViewport).toHaveBeenCalledWith(component.filteredOptions[1]);
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.filteredOptions[1].isActive).toEqual(false);
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[0]);
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[1]);
    expect(component.bringItemToViewport).toHaveBeenCalledWith(component.filteredOptions[0]);

    // Press Backspace
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(8);
    component.statement = '';
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.filteredOptions).toEqual(allOptions);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.filteredOptions[1].isActive).toBeFalsy();
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[0]);

    // Mouse Over 2nd option
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    component.onMouseOver(null, option2);
    expect(component.filteredOptions[0].isActive).toBeFalsy();
    expect(component.filteredOptions[1].isActive).toEqual(true);
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[1]);
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[0]);

    // Mouse Over 1st option
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    component.onMouseOver(null, option1);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.filteredOptions[1].isActive).toBeFalsy();
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[0]);
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[1]);

    let mouseEvent = {movementX: 0, movementY: 0};
    component.onMouseOver(mouseEvent, option2);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.filteredOptions[1].isActive).toBeFalsy();

    // Press Esc
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(27);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.stringTillCaret).toEqual('');
    expect(component.showAutoComplete).toEqual(false);
    expect(component.filteredOptions[0].isActive).toEqual(false);
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[0]);

    // Press Shift+S
    component.statement = 'S';
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(83);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.caretPosition).toEqual(1);
    expect(component.stringTillCaret).toEqual('S');
    expect(component.filteredOptions).toEqual([option3]);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).not.toHaveBeenCalled();

    // Press Enter
    fixture.debugElement.nativeElement.querySelector('.autocomplete').textContent = 'SOME_FUNCTION_3()';
    range.setStart(autoCompleteElement.firstChild, 1);
    range.setEnd(autoCompleteElement.firstChild, 1);
    selection.removeAllRanges();
    selection.addRange(range);
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(13);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.showAutoComplete).toEqual(false);
    expect(component.statement).toEqual('SOME_FUNCTION_3()');
    expect(component.filteredOptions[0].isActive).toEqual(false);
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[0]);

    // Press Right Arrow
    range.setStart(autoCompleteElement.firstChild, 16);
    range.setEnd(autoCompleteElement.firstChild, 16);
    selection.removeAllRanges();
    selection.addRange(range);
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(39);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.filteredOptions).toEqual(allOptions);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.filteredOptions[1].isActive).toBeFalsy();
    expect(component.filteredOptions[2].isActive).toBeFalsy();
    expect(component.filteredOptions[3].isActive).toBeFalsy();
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).not.toHaveBeenCalled();

    // Press f
    fixture.debugElement.nativeElement.querySelector('.autocomplete').textContent = 'SOME_FUNCTION_3()f';
    range.setStart(autoCompleteElement.firstChild, 18);
    range.setEnd(autoCompleteElement.firstChild, 18);
    selection.removeAllRanges();
    selection.addRange(range);
    component.statement = 'SOME_FUNCTION_3()f';
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(70);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.filteredOptions).toEqual([field1]);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).toHaveBeenCalledWith(component.options[0]);

    // Press Down Arrow
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(40);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.filteredOptions).toEqual([field1]);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[0]);
    expect(component.hidePopover).not.toHaveBeenCalled();

    // Press Up Arrow
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    event = this.createEvent(38);
    expect(component.onKeyup(event)).toEqual(false);
    expect(component.filteredOptions).toEqual([field1]);
    expect(component.filteredOptions[0].isActive).toEqual(true);
    expect(component.showPopover).toHaveBeenCalledWith(component.filteredOptions[0]);
    expect(component.hidePopover).not.toHaveBeenCalled();

    // Click option
    fixture.debugElement.nativeElement.querySelector('.autocomplete').textContent = 'SOME_FUNCTION_3()field_1';
    range.setStart(autoCompleteElement.firstChild, 24);
    range.setEnd(autoCompleteElement.firstChild, 24);
    selection.removeAllRanges();
    selection.addRange(range);
    showPopoverSpy.calls.reset();
    hidePopoverSpy.calls.reset();
    expect(component.onClick(field1));
    expect(component.showAutoComplete).toEqual(false);
    expect(component.statement).toEqual('SOME_FUNCTION_3()field_1');
    expect(component.filteredOptions[0].isActive).toEqual(false);
    expect(component.showPopover).not.toHaveBeenCalled();
    expect(component.hidePopover).toHaveBeenCalledWith(component.filteredOptions[0]);

  }));

});
