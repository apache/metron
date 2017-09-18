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
import { Http } from '@angular/http';
import {Component, OnInit, ElementRef, Input, ViewChild} from '@angular/core';
import {FormGroup, Validators, FormControl} from '@angular/forms';
import {ParserExtensionService} from '../../service/parser-extension.service';
import {Router, ActivatedRoute} from '@angular/router';
import {MetronAlerts} from '../../shared/metron-alerts';
import {RestError} from '../../model/rest-error';
import {ExtensionsUploadComponent} from "./extensions-upload.component";

@Component({
  selector: 'metron-install-parser-extension',
  templateUrl: 'extensions-parser-install.component.html',
  styleUrls: ['extensions-parser-install.component.scss']
})

export class ExtensionsParserInstallComponent implements OnInit {

  @ViewChild(ExtensionsUploadComponent) uploadComponent: ExtensionsUploadComponent;

  constructor(private route: ActivatedRoute,
              private router: Router,){
  }


  ngOnInit() {
  }


  goBack() {
    this.router.navigateByUrl('/extensions');
    return false;
  }


}
