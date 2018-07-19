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
# invent a random password for intermediate keys
if [ ! -f $name/pass-pk12 ]; then
  echo $PASSWORD > $name/pass-pk12
else
  PASSWORD=$(cat $name/pass-pk12)
fi

keytool="keytool -noprompt"

openssl pkcs12 -export \
               -in ca/intermediate/certs/$name.cert.pem -inkey ca/intermediate/private/$name.key.pem \
               -passin file:$name/pass -passout file:$name/pass-pk12 \
               -out $name/${name}.p12 -name ${name} \
               -CAfile ca/intermediate/certs/ca-chain.cert.pem -caname CAintermediate

truststorePasswd=$(openssl rand -base64 32)
keystorePasswd=$(openssl rand -base64 32)
keyPasswd=$(openssl rand -base64 32)

echo "$truststorePasswd" > $name/pass-trust
echo "$keystorePasswd" > $name/pass-keystore
echo "$keyPasswd" >> $name/pass-key

# create server keystore
echo "Import Keystore"
${keytool} -importkeystore \
        -deststorepass $keystorePasswd -destkeypass "$keyPasswd" -destkeystore $name/${name}.keystore.jks \
        -srckeystore $name/${name}.p12 -srcstoretype PKCS12 -srcstorepass "$PASSWORD" \
        -destalias ${name}

echo "Import CA"
${keytool} -import -alias CAcert \
        -storepass "$keystorePasswd" -file ca/intermediate/certs/ca-chain.cert.pem \
        -keystore $name/${name}.keystore.jks

# create truststore
echo "Import Truststore"
${keytool} -import -alias CAcert \
        -storepass "$truststorePasswd" -file ca/intermediate/certs/ca-chain.cert.pem \
        -keystore $name/${name}.truststore.jks
