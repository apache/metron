/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing software
 * distributed under the License is distributed on an "AS IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 export class PdmlField {
  public $: any[]
  public field?: PdmlField[]
}

export class PdmlProto {
  public $: any
  public field: PdmlField[]
  
  public static findField(p: PdmlProto, name: string): PdmlField {
    return p.field.find(f => f['$']['name'] == name)
  }
}


export class PdmlPacket {
  public $: any
  public proto: PdmlProto[]
  public expanded: boolean = false
}

export class Pdml {
  public pdml: { S: any, packet: PdmlPacket[] }
}
