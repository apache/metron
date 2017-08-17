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

export class MetronRestApiUtils {

  public static extractColumnNameDataFromRestApi(res: Response): ColumnMetadata[] {
    let response: any = res || {};
    let processedKeys: string[] = [];
    let columnMetadatas: ColumnMetadata[] = [];

    for (let index of Object.keys(response)) {
      let indexMetaData = response[index];
      for (let key of Object.keys(indexMetaData)) {
        if (processedKeys.indexOf(key) === -1) {
          processedKeys.push(key);
          columnMetadatas.push(new ColumnMetadata(key, indexMetaData[key]));
        }
      }
    }

    return columnMetadatas;
  }

}
