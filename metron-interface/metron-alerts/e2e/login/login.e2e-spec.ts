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
import { LoginPage } from './login.po';
import {deleteTestData, loadTestData} from '../utils/e2e_util';

describe('Test spec for login page', function() {
    let page: LoginPage;

    beforeAll(async function() : Promise<any> {
      page = new LoginPage();
      await loadTestData();
    });

    afterAll(async function() : Promise<any> {
        await deleteTestData();
    });

    it('should display error message for invalid credentials', async function() : Promise<any> {
        await page.navigateToLogin();
        await page.setUserNameAndPassword('admin', 'admin');
        await page.submitLoginForm();
        expect(await page.getErrorMessage()).toEqual('Login failed for admin');
    });

    it('should login for valid credentials', async function() : Promise<any> {
        await page.navigateToLogin();
        await page.setUserNameAndPassword('admin', 'password');
        await page.submitLoginForm();
    });

    it('should logout', async function() : Promise<any> {
        await page.logout();
        expect(await page.getLocation()).toEqual('http://localhost:4200/login');
    });
});
