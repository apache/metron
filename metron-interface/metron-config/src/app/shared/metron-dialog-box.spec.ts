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
/* tslint:disable:no-unused-variable */

import {MetronDialogBox} from './metron-dialog-box';

describe('MetronDialogBox', () => {

  it('should create an instance', () => {
    expect(new MetronDialogBox()).toBeTruthy();
  });

  it('should return true', () => {
    let showConfirmationMessage = new MetronDialogBox().showConfirmationMessage('test message');
    let subscription = showConfirmationMessage.subscribe(result => {
      expect(result).toEqual(true);
      subscription.unsubscribe();
    });
    spyOn(showConfirmationMessage, 'subscribe');

    expect($(document).find('.metron-dialog .modal-title').text()).toEqual('Confirmation');
    expect($(document).find('.metron-dialog .modal-body p').text()).toEqual('test message');

    $(document).find('.metron-dialog .btn-primary').click();

    $(document).find('.metron-dialog').trigger('hidden.bs.modal');

    expect($(document).find('.metron-dialog').length).toEqual(0);

  });

  it('should return false', () => {
    let showConfirmationMessage = new MetronDialogBox().showConfirmationMessage('test message');
    let subscription = showConfirmationMessage.subscribe(result => {
      expect(result).toEqual(false);
      subscription.unsubscribe();
    });
    spyOn(showConfirmationMessage, 'subscribe');

    $(document).find('.metron-dialog .form-enable-disable-button').click();

    $(document).find('.metron-dialog').trigger('hidden.bs.modal');

    expect($(document).find('.metron-dialog').length).toEqual(0);

  });
});

