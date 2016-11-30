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
import {Component, Input, Output, EventEmitter, ElementRef, ViewChild, HostListener} from '@angular/core';
import {AutocompleteStatementGenerator} from './autocomplete-statement-generator';
import {StringUtil} from '../../util/stringUtils';
import {AutocompleteOption} from '../../model/autocomplete-option';
declare var $: any;

@Component({
  selector: 'metron-config-autocomplete',
  templateUrl: 'autocomplete.component.html',
  styleUrls: ['autocomplete.component.scss']
})

/*
 @Input() statement: string; -> The input statement for autocomplete
 @Input() functions: {}; -> Map containing key value pairs for auto complete keys are displayed for dropdown
 @Input() userInputMap: string[]; -> Map that will hold a static string to a regex, the regex is applied on user input
    and the static string is shown if the regex matches
 @Input() height: number; -> Height of the auto complete div

 @Output() result: EventEmitter<string> = new EventEmitter<string>(); -> The event will emmit the statement after auto complete
 */
export class AutocompleteComponent {

  @Input() id: string = '';
  @Input() statement: string;
  @Input() options: AutocompleteOption[];
  @Input() height: number;
  @Input() autocompleteStatementGenerator: AutocompleteStatementGenerator;

  @Output() result: EventEmitter<string> = new EventEmitter<string>();

  @ViewChild('dropdown') dropdown: ElementRef;
  @ViewChild('autocomplete') autocomplete: ElementRef;
  @ViewChild('autocompleteSelect') autocompleteSelect: ElementRef;

  showAutoComplete: boolean;
  stringTillCaret: string = '';
  caretPosition: number = 0;
  filteredOptions: AutocompleteOption[] = [];

  canProcessKeyup(): boolean {
    // Need not handle ctrl+A kind of stuff
    let range = window.getSelection().getRangeAt(0);
    return range.startOffset === range.endOffset;
  }

  storeCaretPosition() {
    let caretOffset = 0;
    if (typeof window.getSelection !== 'undefined' && window.getSelection().rangeCount > 0) {
      let range = window.getSelection().getRangeAt(0);
      caretOffset = range.startOffset;
    }
    this.caretPosition = caretOffset;
  }

  filter(options: AutocompleteOption[], filter: string): AutocompleteOption[] {
    let indexOfLastNonAlphaNumeric = StringUtil.getIndexOfLastNonAlphaNumeric(filter);
    let userTypedString = filter.substr(indexOfLastNonAlphaNumeric + 1, filter.length);
    return options.filter((option) => {
      return option.name.startsWith(userTypedString);
    });
  }

  getActiveOption(): AutocompleteOption {
    return this.options.find((option: AutocompleteOption) => option.isActive === true);
  }

  selectOption(option: AutocompleteOption) {
    if (option) {
      option.isActive = true;
      this.showPopover(option);
    }
  }

  deselectOption(option: AutocompleteOption) {
    if (option) {
      option.isActive = false;
      this.hidePopover(option);
    }
  }

  selectFirstFunction() {
    this.deselectOption(this.getActiveOption());
    if (this.filteredOptions.length > 0) {
      this.filteredOptions[0].isActive = true;
    }
  }

  selectPreviousFunction(currentOption: AutocompleteOption) {
    if (this.filteredOptions.length > 1) {
      let activeIndex = this.filteredOptions.indexOf(currentOption);
      let previousOption: AutocompleteOption;
      if (activeIndex === 0) {
        previousOption = this.filteredOptions[this.filteredOptions.length - 1];
      } else {
        previousOption = this.filteredOptions[activeIndex - 1];
      }
      this.selectOption(previousOption);
      this.deselectOption(currentOption);
      this.bringItemToViewport(previousOption);
    } else {
      this.showPopover(currentOption);
    }
  }

  selectNextFunction(currentOption: AutocompleteOption) {
    if (this.filteredOptions.length > 1) {
      let currentIndex = this.filteredOptions.indexOf(currentOption);
      let nextOption: AutocompleteOption;
      if (currentIndex === this.filteredOptions.length - 1) {
        nextOption = this.filteredOptions[0];
      } else {
        nextOption = this.filteredOptions[currentIndex + 1];
      }
      this.selectOption(nextOption);
      this.deselectOption(currentOption);
      this.bringItemToViewport(nextOption);
    } else {
      this.showPopover(currentOption);
    }
  }

  getCaretCoordinates(): {x: number, y: number} {
    let range, rects, rect;
    let x = 0, y = 0;

    if (window.getSelection && window.getSelection().rangeCount) {
      range = window.getSelection().getRangeAt(0).cloneRange();
      if (range.getClientRects) {
        range.collapse(true);
        rects = range.getClientRects();
        if (rects.length > 0) {
          rect = rects[0];
          x = rect.left;
          y = rect.top;
        }
      }
    }
    return {x: x, y: y};
  }

  setAutocompleteDropdownPosition(): void {
    let caretCoordinates = this.getCaretCoordinates();
    if (caretCoordinates.x > 0 && caretCoordinates.y > 0) {
      let autoCompleteDivCoordinates = this.autocomplete.nativeElement.getClientRects()[0];

      // 200 is the width of the dropdown - we need to ensure the dropdown is not chopped off on the right,
      // dropdown should be there in the div
      let left = (caretCoordinates.x - autoCompleteDivCoordinates.left) - 22;
      left = ((left + 200) < autoCompleteDivCoordinates.width) ? left : (autoCompleteDivCoordinates.width - 200);

      this.autocompleteSelect.nativeElement.style.left = left + 'px';
      this.autocompleteSelect.nativeElement.style.top = (caretCoordinates.y - autoCompleteDivCoordinates.top) + 20 + 'px';
    } else {
      this.showAutoComplete = false;
    }
  }

  emitResult(): void {
    this.result.emit(decodeURIComponent(encodeURIComponent(this.statement.trim())));
  }

  appendToStatement(option: AutocompleteOption) {
    let strLHSOfCaret = this.statement.substr(0, this.caretPosition);
    let strRHSOfCaret = this.statement.substr(this.caretPosition);

    strLHSOfCaret = this.removeUserTypedString(strLHSOfCaret);
    let signature = this.autocompleteStatementGenerator.getFunctionSignature(strLHSOfCaret, option, strRHSOfCaret);

    this.statement = strLHSOfCaret + signature + strRHSOfCaret;
    this.showAutoComplete = false;
    this.emitResult();
    this.setCaretWrapper(this.autocompleteStatementGenerator.getIndexOfNextToken(strLHSOfCaret + signature, this.caretPosition));
  }

  setCaretWrapper(position: number) {
    setTimeout(() => {
      this.setCaret(position);
    });
  }

  setCaret(offsetWithin: number) {
    this.autocomplete.nativeElement.focus();
    let textNode = this.autocomplete.nativeElement.firstChild;
    if (textNode) {
      let caret = offsetWithin;
      let range = document.createRange();
      range.setStart(textNode, caret);
      range.setEnd(textNode, caret);
      let sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(range);
    }
  }

  removeUserTypedString(str: string): string {
    let indexOfLastNonAlphaNumeric = StringUtil.getIndexOfLastNonAlphaNumeric(str);
    return str.substr(0, indexOfLastNonAlphaNumeric + 1);
  }

  bringItemToViewport(autocompleteOption: AutocompleteOption) {
    let currentElement = document.getElementById((this.id ? this.id : '') + '-' + autocompleteOption.name);
    if (currentElement) {
      let currentElementOffsetBottom = currentElement.offsetTop + currentElement.offsetHeight;
      if (currentElementOffsetBottom >= 200) { // item is not in view port, the height of popup is just 200
        let itemsOnTopOfCurrentOutOfViewport = (currentElementOffsetBottom - 200) / 24; // 24 is average height of each item
        let scrollTop = (itemsOnTopOfCurrentOutOfViewport * 24) + 24;
        this.autocompleteSelect.nativeElement.scrollTop = scrollTop;
      } else {
        this.autocompleteSelect.nativeElement.scrollTop = 0;
      }
    }

    // var currentItemIndex = this.filteredOptions.indexOf(autocompleteOption);
    // currentItemIndex = currentItemIndex >= 8 ? currentItemIndex-8 : 0;
    // this.autocompleteSelect.nativeElement.scrollTop = (currentItemIndex*23);
  }

  showPopover(autocompleteOption: AutocompleteOption) {
    $('#' + this.id + '-' + autocompleteOption.name).popover('show');
  }

  hidePopover(autocompleteOption: AutocompleteOption) {
    $('#' + this.id + '-' + autocompleteOption.name).popover('dispose');
  }

  @HostListener('keydown', ['$event'])
  onKeyDown($event) {
    if ($event.keyCode === 13 && this.showAutoComplete) {
      $event.stopPropagation();
      return false;
    }

    if ($event.keyCode === 40 || $event.keyCode === 38) {
      $event.stopPropagation();
      $event.preventDefault();
      $event.stopImmediatePropagation();
      return false;
    }
  }

  @HostListener('keyup', ['$event'])
  onKeyup($event) {
    $event.stopPropagation();
    $event.preventDefault();
    $event.stopImmediatePropagation();

    if (!this.canProcessKeyup()) {
      return;
    }

    this.emitResult();
    this.storeCaretPosition();
    this.setAutocompleteDropdownPosition();

    this.stringTillCaret = this.statement.substr(0, this.caretPosition);
    this.filteredOptions = this.filter(this.options, this.stringTillCaret);
    this.showAutoComplete = this.filteredOptions.length > 0;

    if (!$event.ctrlKey && $event.keyCode === 17 || $event.key === 'Shift') {
      return false;
    }  else if ($event.keyCode === 13) {
      // Enter key -> get current selected value from dropdown and append to statement
      let activeOption = this.getActiveOption();
      this.appendToStatement(activeOption);
      this.deselectOption(activeOption);
      return false;
    } else if ($event.keyCode === 32 || $event.keyCode === 37 || $event.keyCode === 39) {
      this.selectFirstFunction();
    } else if ($event.keyCode === 40) {
      // Down Arrow  -> Remove selection from current item and select next item
      this.selectNextFunction(this.getActiveOption());
    } else if ($event.keyCode === 38) {
      // Up Arrow  -> Remove selection from current item and select previous item
      this.selectPreviousFunction(this.getActiveOption());
    } else if ($event.keyCode === 27) {
      // Esc key -> Hide autocomplete
      this.showAutoComplete = false;
      this.deselectOption(this.getActiveOption());
    } else if ($event.keyCode === 8) {
      // Backspace key -> Move caret back
      this.selectFirstFunction();
      this.setCaretWrapper(this.statement[this.caretPosition - 1] === ' ' ? this.caretPosition - 1 : this.caretPosition);
    } else {
      this.selectFirstFunction();
    }

    return false;
  }

  onClick(option: AutocompleteOption) {
    this.appendToStatement(option);
    this.deselectOption(option);
  }

  onMouseOver($event, option: AutocompleteOption) {
    if ($event && $event.movementX === 0 && $event.movementY === 0) {
      return;
    }

    let activeOption = this.getActiveOption();
    if (option !== activeOption) {
      this.deselectOption(activeOption);
      option.isActive = true;
    }
    this.showPopover(option);
  }
}
