#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

name=$1
PASSWORD=$KNOX_PASSWORD

keytool="keytool -noprompt"

echo "Making PKCS12:"
openssl pkcs12 -export \
               -in $name/${name}.cert.pem -inkey $name/${name}.key.pem \
               -passin file:$name/pass -passout "pass:$PASSWORD" \
               -out $name/${name}.p12 -name ${name} \
               -CAfile ca/intermediate/certs/intermediate.cert.pem -caname CAintermediate

echo "Importing to keystore"
${keytool} -importkeystore  \
  -srckeystore $name/${name}.p12 -srcstoretype PKCS12 -srcstorepass "$PASSWORD" \
  -destkeystore gateway.jks -deststoretype jks -deststorepass "$KNOX_PASSWORD" \
  -srcalias $name$DNS -destalias gateway-identity

echo "Adding the CA chain"
${keytool} -import -alias CAcert \
        -storepass $KNOX_PASSWORD -file ca/intermediate/certs/ca-chain.cert.pem \
        -keystore gateway.jks

echo "Now put the gateway.jks file in /usr/hdp/current/knox-server/data/security/keystores" 
echo "and run:"
echo "/usr/hdp/current/knox-server/bin/knoxcli.sh create-alias gateway-identity-passphrase --value <KNOX_PASSWORD>"