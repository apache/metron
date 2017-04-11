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

describe('login to application', function() {
    let page: LoginPage;

    beforeEach(() => {
        page = new LoginPage();
    });

    it('should display error message for invalid credentials', () => {
        page.navigateToLogin();
        page.setUserNameAndPassword('admin', 'admin');
        page.submitLoginForm();
        expect(page.getErrorMessage()).toEqual('Login failed for admin');
    });

    it('should login for valid credentials', () => {
        page.navigateToLogin();
        page.setUserNameAndPassword('admin', 'password');
        page.submitLoginForm();
        expect(page.getLocation()).toEqual('http://localhost:4200/sensors');
    });

    it('should logout', () => {
        page.logout();
        expect(page.getLocation()).toEqual('http://localhost:4200/login');
    });
});
