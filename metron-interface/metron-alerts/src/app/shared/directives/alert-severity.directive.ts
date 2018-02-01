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
import { Directive, ElementRef, OnChanges, SimpleChanges, Input, OnInit } from '@angular/core';

@Directive({
  selector: '[appAlertSeverity]'
})
export class AlertSeverityDirective implements OnInit, OnChanges {

  @Input() severity: number;

  constructor(private el: ElementRef) { }

  ngOnInit() {
    this.setBorder(this.severity);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['severity'] && changes['severity'].currentValue) {
      this.setBorder(this.severity);
    }
  }

  private setBorder(severity: number) {

    if ( severity > 69 ) {
      this.el.nativeElement.style.borderLeft = '3px solid #D60A15';
      this.el.nativeElement.style.paddingLeft = '5px';
    } else if ( severity > 39 ) {
      this.el.nativeElement.style.borderLeft = '3px solid #D6711D';
      this.el.nativeElement.style.paddingLeft = '5px';
    } else  {
      this.el.nativeElement.style.borderLeft = '3px solid #AC9B5A';
      this.el.nativeElement.style.paddingLeft = '5px';
    }
  }

}
