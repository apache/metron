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
import {ColumnMetadata} from '../model/column-metadata';

export class MetadataUtil {

  private static createColumMetaData(properties: any, columnMetadata: ColumnMetadata[], seen: string[]) {
     try {
       let columnNames = Object.keys(properties);
       for (let columnName of columnNames) {
         if (seen.indexOf(columnName) === -1) {
           seen.push(columnName);
           columnMetadata.push(
             new ColumnMetadata(columnName, (properties[columnName].type ? properties[columnName].type.toUpperCase() : ''))
           );
         }
       }
     } catch (e) {}
  }

  public static extractData(res: Response): any {
    let response: any = res || {};
    let columnMetadata = [];
    let seen: string[] = [];

    for (let index in response.metadata.indices) {
      if (index.startsWith('bro') || index.startsWith('bro') || index.startsWith('bro')) {
        let mappings = response.metadata.indices[index].mappings;
        for (let type of Object.keys(mappings)) {
          MetadataUtil.createColumMetaData(response.metadata.indices[index].mappings[type].properties, columnMetadata, seen);
        }
      }
    }

    columnMetadata.push(new ColumnMetadata('_id', 'string'));
    return columnMetadata;
  }

  public static extractESErrorMessage(error: any): any {
    let message = error.error.reason;
    error.error.root_cause.map(cause => {
      message += '\n' + cause.index + ': ' + cause.reason;
    });

    return message;
  }
}
