import {NgModule} from '@angular/core';

import {BlocklyEditorComponent}   from './blockly-editor.component';
import {SharedModule} from "../shared.module";

@NgModule({
  imports: [ SharedModule ],
  exports: [ BlocklyEditorComponent ],
  declarations: [ BlocklyEditorComponent ],
  providers: [],
})
export class BlocklyEditorModule {
}