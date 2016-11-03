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
var parse =function(rawMessage){
	parts=rawMessage.split("|");
	var message = new MetronMessage("userlog");
	message["customerId"]=parts[1];
	message["first_name"]=parts[2];
	message["last_name"]=parts[3];
	message["age"]=parts[4];
	message["login-time"]=parts[0];
	message["ip-address"]=parts[5];
	message["os"]=parts[6];
	message["device"]=parts[7];
	return message;
};