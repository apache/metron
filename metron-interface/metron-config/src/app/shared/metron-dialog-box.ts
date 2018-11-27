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
import { EventEmitter }     from '@angular/core';

export class MetronDialogBox {

  private createDialogBox(message: string, title: string) {

    let html = `<div class="metron-dialog modal fade"  data-backdrop="static" >
                  <div class="modal-dialog modal-sm" role="document">
                    <div class="modal-content">
                      <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                        <span class="modal-title"><b>` + title + `</b></span>
                      </div>
                      <div class="modal-body">
                        <p>` +  message + `</p>
                      </div>
                      <div class="modal-footer">
                        <button type="button" class="btn btn-primary">OK</button>
                        <button type="button" class="btn form-enable-disable-button ml-1" data-dismiss="modal">Cancel</button>
                      </div>
                    </div>
                  </div>
                </div>`;

    let element = document.createElement('div');
    element.innerHTML = html;

    document.body.appendChild(element);

    return element;
  }

  public showConfirmationMessage(message: string): EventEmitter<boolean> {
    let eventEmitter = new EventEmitter<boolean>();
    let element = this.createDialogBox(message, 'Confirmation');

    $(element).find('.metron-dialog').modal('show');

    $(element).find('.btn-primary').on('click', function (e) {
      $(element).find('.metron-dialog').modal('hide');
      eventEmitter.emit(true);
    });

    $(element).find('.form-enable-disable-button').on('click', function (e) {
      $(element).find('.metron-dialog').modal('hide');
      eventEmitter.emit(false);
    });

    $(element).find('.metron-dialog').on('hidden.bs.modal', function (e) {
      $(element).remove();
    });

    return eventEmitter;

  }
}
