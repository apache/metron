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
import { Component, AfterViewInit, ViewChild, ElementRef, forwardRef, Input} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {AutocompleteOption} from '../../model/autocomplete-option';

declare var ace: any;

@Component({
  selector: 'metron-config-ace-editor',
  templateUrl: 'ace-editor.component.html',
  styleUrls: ['ace-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AceEditorComponent),
      multi: true
    }
  ]
})
export class AceEditorComponent implements AfterViewInit, ControlValueAccessor {

  inputJson: any = '';
  aceConfigEditor: AceAjax.Editor;
  @Input() type = 'JSON';
  @Input() placeHolder = 'Enter text here';
  @Input() options: AutocompleteOption[] = [];
  @ViewChild('aceEditor') aceEditorEle: ElementRef;

  private onTouchedCallback;
  private onChangeCallback;

  constructor() {
    ace.config.set('basePath', 'assets/ace');
  }

  ngAfterViewInit() {
    ace.config.loadModule('ace/ext/language_tools',  () => { this.initializeEditor(); });
  }

  writeValue(obj: any) {
    this.inputJson = obj;
    this.setInput();
  }

  registerOnChange(fn: any) {
    this.onChangeCallback = fn;
  }

  registerOnTouched(fn: any) {
    this.onTouchedCallback = fn;
  }

  setDisabledState(isDisabled: boolean) {
    // TODO set readonly
  }

  initializeEditor() {
    this.aceConfigEditor = this.createEditor(this.aceEditorEle.nativeElement);
    this.addPlaceHolder();
    this.setInput();
  }

  updatePlaceHolderText() {
    let shouldShow = !this.aceConfigEditor.session.getValue().length;
    let node = this.aceConfigEditor.renderer['emptyMessageNode'];
    if (!shouldShow && node) {
      this.aceConfigEditor.renderer.scroller.removeChild(this.aceConfigEditor.renderer['emptyMessageNode']);
      this.aceConfigEditor.renderer['emptyMessageNode'] = null;
    } else if (shouldShow && !node) {
      node = this.aceConfigEditor.renderer['emptyMessageNode'] = document.createElement('div');
      node.textContent = this.placeHolder;
      node.className = 'ace_invisible ace_emptyMessage';
      this.aceConfigEditor.renderer.scroller.appendChild(node);
    }
  }

  addPlaceHolder() {
    this.aceConfigEditor.on('input', () => { this.updatePlaceHolderText(); });
    setTimeout(() => { this.updatePlaceHolderText(); }, 100);
  }

  private createEditor(element: ElementRef) {
    let parserConfigEditor = ace.edit(element);
    parserConfigEditor.getSession().setMode(this.getEditorType());
    parserConfigEditor.getSession().setTabSize(2);
    parserConfigEditor.getSession().setUseWrapMode(true);
    parserConfigEditor.getSession().setWrapLimitRange(72, 72);

    parserConfigEditor.$blockScrolling = Infinity;
    parserConfigEditor.setTheme('ace/theme/monokai');
    parserConfigEditor.setOptions({
      minLines: 10,
      highlightActiveLine: false,
      maxLines: Infinity,
      enableBasicAutocompletion: true,
      enableSnippets: true,
      enableLiveAutocompletion: true
    });
    parserConfigEditor.on('change', (e: any) => {
      this.inputJson = this.aceConfigEditor.getValue();
      this.onChangeCallback(this.aceConfigEditor.getValue());
    });

    if (this.type === 'GROK') {
      parserConfigEditor.completers = [this.getGrokCompletion()];
    }

    return parserConfigEditor;
  }

  private getGrokCompletion() {
    let _this = this;
    return {
      getCompletions: function(editor, session, pos, prefix, callback) {
        let autoCompletePrefix = '';
        let autoCompleteSuffix = '';
        let options = _this.options;

        let currentToken = editor.getSession().getTokenAt(pos.row, pos.column);

        // No value or user typed just a char
        if (currentToken === null || currentToken.type === 'comment') {
          autoCompletePrefix = '%{';
          autoCompleteSuffix = ':$0}';
        } else {
          // }any<here>
          if (currentToken.type === 'invalid') {
            let lastToken = editor.getSession().getTokenAt(pos.row, (pos.column - currentToken.value.length));
            autoCompletePrefix = lastToken.value.endsWith('}') ? ' %{' : '%{';
            autoCompleteSuffix = ':$0}';
          }

          // In %{<here>}
          if (currentToken.type === 'paren.rparen') {
            autoCompletePrefix = currentToken.value.endsWith(' ') ? '%{' : ' %{';
            autoCompleteSuffix = ':$0}';
          }

          // %{NUM<here>:}
          if (currentToken.type === 'paren.lparen' || currentToken.type === 'variable') {
            let nextToken = editor.getSession().getTokenAt(pos.row, pos.column + 1);
            autoCompletePrefix = '';
            autoCompleteSuffix = (nextToken && nextToken.value.indexOf(':') > -1) ? '' : ':$0}';
          }

          // %{NUMBER:<here>}
          if (currentToken.type === 'seperator' || currentToken.type === 'string') {
            let autocompleteVal = currentToken.value.replace(/:/g, '');
            let autocompletes = autocompleteVal.length === 0 ? 'variable' : '';
            options = [new AutocompleteOption(autocompletes)];
          }
        }

        callback(null, options.map(function(autocompleteOption) {
          return {
            caption: autocompleteOption.name,
            snippet: autoCompletePrefix + autocompleteOption.name + autoCompleteSuffix,
            meta: 'grok-pattern',
            score: Number.MAX_VALUE
          };
        }));

      }
    };
  }

  private getEditorType() {
      if (this.type === 'GROK') {
        return 'ace/mode/grok';
      }

      return 'ace/mode/json';
  }

  private setInput() {
      if (this.aceConfigEditor && this.inputJson) {
        this.aceConfigEditor.getSession().setValue(this.inputJson);
        this.aceConfigEditor.resize(true);
      }
  }

}
