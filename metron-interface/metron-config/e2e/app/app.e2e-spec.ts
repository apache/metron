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
import { AppPage } from './app.po';
import {LoginPage} from '../login/login.po';

describe('Application Skeleton', function() {
  let page = new AppPage();
  let loginPage = new LoginPage();

  beforeAll(() => {
    loginPage.login();
  });

  afterAll(() => {
    loginPage.logout();
  });

  it('should have metron logo', () => {
    expect(page.isMetronLogoPresent()).toEqual(true);
  });

  it('should have navigations', () => {
    expect(page.getNavigationTitle()).toEqual('Operations');
    expect(page.getNavigationLinks()).toEqual([ 'Sensors', 'General Settings' ]);
  });

  it('should navigate to all pages', () => {
    expect(page.selectNavLink('General Settings')).toEqual(true);
    expect(page.getUrl()).toEqual('http://localhost:4200/general-settings');

    expect(page.selectNavLink('Sensors')).toEqual(true);
    expect(page.getUrl()).toEqual('http://localhost:4200/sensors');
  });
  
});
