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

generate_and_sign_cert() {
      name=$1
      dns=$1
      subject="${SUBJ_BASE}CN=${name}"

      mkdir $name

      # invent a random password and store it
      password_file=$name/pass
      password=$(openssl rand -base64 32)
      echo $password > $password_file

      echo "Creating key for $name:"
      openssl genrsa -aes256 \
            -passout file:$password_file \
            -out ca/intermediate/private/$name.key.pem 2048 
            
      chmod 400 ca/intermediate/private/$name.key.pem

      cat <(cat ca/intermediate/openssl.cnf \
                  <(printf "\n[ alt_names ]\nDNS.1 =$dns"))

      echo "Creating signing request for $name:"
      openssl req -config ca/intermediate/openssl.cnf \
            -key ca/intermediate/private/$name.key.pem \
            -passin file:$password_file \
            -new -sha256 \
            -reqexts server_cert -config <(cat ca/intermediate/openssl.cnf \
                  <(printf "\n[ alt_names ]\nDNS.1=$dns")) \
            -out ca/intermediate/csr/$name.csr.pem \
            -subj $subject

      echo "Signing $name cert:"
      openssl ca \
            -config <(cat ca/intermediate/openssl.cnf \
                  <(printf "\n[ alt_names ]\nDNS.1 = $dns")) \
            -extensions server_cert -days 3750 -notext -md sha256 \
            -passin file:pass_intermediate \
            -in ca/intermediate/csr/$name.csr.pem \
            -out ca/intermediate/certs/$name.cert.pem \
            -batch 
      
      cp ca/intermediate/certs/$name.cert.pem $name/
      cp ca/intermediate/private/$name.key.pem $name/
      cp ca/intermediate/certs/ca-chain.cert.pem $name/
}

generate_and_sign_cert $1
