import { Component, AfterViewInit, ViewChild, ElementRef, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import Editor = AceAjax.Editor;

declare var ace: any;

@Component({
  selector: 'metron-config-ace-editor',
  templateUrl: './ace-editor.component.html',
  styleUrls: ['./ace-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AceEditorComponent),
      multi: true
    }
  ]
})
export class AceEditorComponent implements AfterViewInit, ControlValueAccessor{

  inputJson: any;
  aceConfigEditor: Editor;
  @Input() type: string = 'JSON';
  @ViewChild('aceEditor') aceEditorEle: ElementRef;

  private onTouchedCallback;
  private onChangeCallback;

  constructor() {
    ace.config.set('basePath', '/assets/ace');
  }

  ngAfterViewInit() {
    let __this = this;
    ace.config.loadModule('ace/ext/language_tools',  function() {
      __this.aceConfigEditor = __this.initializeEditor(__this.aceEditorEle.nativeElement);
    });
    this.setInput();
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

  private initializeEditor(element: ElementRef) {
    let parserConfigEditor = ace.edit(element);
    parserConfigEditor.setTheme('ace/theme/monokai');
    parserConfigEditor.getSession().setMode(this.getEditorType());
    parserConfigEditor.getSession().setTabSize(2);
    parserConfigEditor.getSession().setUseWrapMode(true);
    parserConfigEditor.getSession().setWrapLimitRange(72, 72);
    parserConfigEditor.setOptions({
      minLines: 25
    });
    parserConfigEditor.$blockScrolling = Infinity;
    parserConfigEditor.setOptions({
      maxLines: Infinity
    });
    parserConfigEditor.setOptions(
        {
          enableBasicAutocompletion: true,
          enableSnippets: true,
          enableLiveAutocompletion: false
        }
    );
    parserConfigEditor.on('change', (e:any) => {
      this.onChangeCallback(this.aceConfigEditor.getValue());
    });

    return parserConfigEditor;
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
