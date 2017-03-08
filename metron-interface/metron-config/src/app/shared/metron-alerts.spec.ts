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

import {MetronAlerts} from './metron-alerts';

describe('MetronAlerts', () => {

  beforeEach(function() {
    MetronAlerts.SUCESS_MESSAGE_DISPALY_TIME = 500;
  });

  afterEach(function() {
    MetronAlerts.SUCESS_MESSAGE_DISPALY_TIME = 5000;
  });

  it('should create an instance', () => {
    expect(new MetronAlerts()).toBeTruthy();
  });

  it('should close success message after timeout', (done) => {
    new MetronAlerts().showSuccessMessage('test message');

    expect($(document).find('.alert.alert-success span').text()).toEqual('test message');

    setTimeout(() => {
      expect($(document).find('.alert .alert-success').length).toEqual(0);
      done();
    }, MetronAlerts.SUCESS_MESSAGE_DISPALY_TIME);
  });

  it('should close success message on click of close', (done) => {
    new MetronAlerts().showSuccessMessage('test message');

    expect($(document).find('.alert.alert-success span').text()).toEqual('test message');

    $(document).find('.alert.alert-success .close').click();

    expect($(document).find('.alert .alert-success').length).toEqual(0);

    setTimeout(() => {
      expect($(document).find('.alert .alert-success').length).toEqual(0);
      done();
    }, MetronAlerts.SUCESS_MESSAGE_DISPALY_TIME);

  });

  // it('should close error message on click of close', () => {
  //   new MetronAlerts().showErrorMessage('test message');
  //
  //   expect($(document).find('.alert.alert-danger span').text()).toEqual('test message');
  //
  //   $(document).find('.alert.alert-danger .close').click();
  //
  //   expect($(document).find('.alert .alert-danger').length).toEqual(0);
  //
  // });

});

