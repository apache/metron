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
import {ElasticsearchUtils} from '../utils/elasticsearch-utils';

export class Filter {
  field: string;
  value: string;
  display: boolean;

  constructor(field: string, value: string, display = true) {
    this.field = field;
    this.value = value;
    this.display = display;
  }

  getQueryString(): string {
    return ElasticsearchUtils.escapeESField(this.field) + ':' +  ElasticsearchUtils.escapeESValue(this.value);
  }
}

export class RangeFilter extends Filter {
  gte: number;
  lte: number;

  constructor(field:string, gte:number, lte:number, display = true) {
    super(field, '', display);
    this.gte = gte;
    this.lte = lte;
    this.value = this.getFilterValue();
  }

  getQueryString(): string {
    return ElasticsearchUtils.escapeESField(this.field) + ':' +  this.getFilterValue();
  }

  getFilterValue() {
    return '(>=' + this.gte + ' AND ' + ' <=' + this.lte + ')';
  }
}
