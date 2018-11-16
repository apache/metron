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
/// <reference path="../../../../node_modules/@types/ace/index.d.ts" />
import {Directive, ElementRef, EventEmitter, AfterViewInit, Output, Input, OnChanges, SimpleChanges} from '@angular/core';

declare var ace: any;
let ACERange = ace.require('ace/range').Range;

@Directive({
  selector: '[appAceEditor]'
})

export class AlertSearchDirective implements AfterViewInit, OnChanges {
  editor: AceAjax.Editor;
  closeButton: any;
  mouseEventTimer: number;
  target: any;

  @Input() text = '';
  @Output() textChanged = new EventEmitter();

  constructor(private elementRef: ElementRef) {
    const el = elementRef.nativeElement;
    el.classList.add('editor');

    ace.config.set('basePath', 'assets/ace');

    this.editor = ace.edit(el);
    this.editor.$blockScrolling = Infinity;
    this.editor.renderer.setShowGutter(false);
    this.editor.renderer.setShowPrintMargin(false);
    this.editor.renderer.setPadding(10);
    this.editor.setTheme('ace/theme/monokai');
    this.editor.container.style.lineHeight = '1.5';

    this.editor.setOptions({
      minLines: 1,
      highlightActiveLine: false,
      maxLines: Infinity,
      fontSize: '0.75em'
    });

    this.editor.getSession().setMode('ace/mode/lucene');

    // This is a hack: setScrollMargin is not available in latest ace typings but is available in ace
    let renderer: any = this.editor.renderer;
    renderer.setScrollMargin(12, 12);

    this.closeButton = document.createElement('i');
    this.closeButton.classList.add('fa');
    this.closeButton.classList.add('fa-times');
    this.editor.on('click', (event) => {
      if (event.domEvent.target.classList.contains('fa-times')) {
        let pos = event.getDocumentPosition();
        let strToDelete = this.getTextTillOperator(event.domEvent.target.parentElement);

        let endIndex = pos.column;
        let startIndex = pos.column - (strToDelete.length + 1);
        if ( startIndex < 0) {
          startIndex = 0;
          endIndex = (strToDelete.length + 1);
        }

        let range = new ACERange(0, startIndex , 0, endIndex);
        this.editor.selection.addRange(range);
        this.editor.removeWordLeft();
        this.editor.renderer.showCursor();
        this.textChanged.next(this.editor.getValue());
      }
    });
  }

  private  getTextTillOperator(valueElement) {
    let str = valueElement ? valueElement.textContent : '';

    let previousSibling = valueElement && valueElement.previousSibling;
    if (previousSibling && previousSibling.classList && previousSibling.classList.contains('ace_keyword')) {
      str = previousSibling.textContent + str;
    }

    previousSibling = previousSibling && previousSibling.previousSibling;
    if (previousSibling && previousSibling.nodeName === '#text') {
      str = previousSibling.textContent + str;
    }

    previousSibling = previousSibling && previousSibling.previousSibling;
    if (previousSibling && previousSibling.classList && previousSibling.classList.contains('ace_operator')) {
      str = previousSibling.textContent + str;
    } else {
      str = str + this.getTextTillNextOperator(valueElement);
    }

    return str;
  }

  getTextTillNextOperator(valueElement) {
    let str = '';
    let nextSibling = valueElement.nextSibling;

    if (nextSibling && nextSibling.nodeName === '#text') {
      str = str + nextSibling.textContent;
    }

    nextSibling = nextSibling && nextSibling.nextSibling;
    if (nextSibling && nextSibling.classList && nextSibling.classList.contains('ace_operator')) {
      str = str + nextSibling.textContent;
    }

    return str;
  }

  getSeacrhText(): string {
    return this.editor.getValue();
  }

  private handleMouseEvent (callback: Function) {
    clearTimeout(this.mouseEventTimer);
    this.mouseEventTimer = window.setTimeout(() => { callback(); }, 100);
  }

  private mouseover($event) {
    if ($event.target.classList.contains('ace_value') ||
        $event.target.classList.contains('ace_keyword') ||
        $event.target.classList.contains('fa-times')) {
      this.handleMouseEvent(() => {
        this.target = $event.target.classList.contains('fa-times') ? $event.target.parentElement : $event.target;
        if (this.target.classList.contains('ace_value')) {
          this.target.classList.add('active');
          if (this.target.previousSibling && this.target.previousSibling.classList) {
            this.target.previousSibling.classList.add('active');
          }
          this.target.appendChild(this.closeButton);
          this.editor.renderer.hideCursor();
        }
        if (this.target.classList.contains('ace_keyword') && !this.target.classList.contains('ace_operator')) {
          this.target.classList.add('active');
          if (this.target.nextSibling && this.target.nextSibling.classList) {
            this.target.nextSibling.classList.add('active');
            this.target.nextSibling.appendChild(this.closeButton);
          }
          this.editor.renderer.hideCursor();
        }
      });
    }
  }

  private mouseout($event) {
    if (this.target) {
      this.handleMouseEvent(() => {
        if (this.target.classList.contains('ace_value')) {
          this.target.classList.remove('active');
          if (this.target.previousSibling && this.target.previousSibling.classList) {
            this.target.previousSibling.classList.remove('active');
          }
          this.target.removeChild(this.closeButton);
          this.editor.renderer.showCursor();
        }
        if (this.target.classList.contains('ace_keyword') && !this.target.classList.contains('ace_operator')) {
          this.target.classList.remove('active');
          if (this.target.nextSibling && this.target.nextSibling.classList) {
            this.target.nextSibling.classList.remove('active');
            this.target.nextSibling.removeChild(this.closeButton);
          }
          this.editor.renderer.showCursor();
        }
        this.target = null;
      });
    }
  }

  ngAfterViewInit() {
    this.editor.getSession().setUseWrapMode(true);
    this.editor.keyBinding.addKeyboardHandler( (data, hashId, keyString, keyCode, event) => {
      if (keyCode === 13) {
        event.preventDefault();
        event.stopPropagation();
        this.textChanged.next(this.editor.getValue());
        return false;
      }
      return true;
    }, 0);
    this.elementRef.nativeElement.querySelector('.ace_content').addEventListener('mouseover', this.mouseover.bind(this));
    this.elementRef.nativeElement.querySelector('.ace_content').addEventListener('mouseout', this.mouseout.bind(this));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes && changes['text']) {
      this.editor.setValue(this.text);
      this.editor.clearSelection();
      this.editor.focus();
    }
  }
}
