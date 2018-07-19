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

make_ca () {
  for a in certs crl newcerts private csr
  do
    mkdir $1/$a
  done
  chmod 700 $1/private
  touch $1/index.txt
  echo 1000 > $1/serial
}

[ ! -f  pass_ca ] && openssl rand -base64 32 > pass_ca
[ ! -f  pass_intermediate ] && openssl rand -base64 32 > pass_intermediate

# create root key
mkdir ca

cp openssl.cnf ca/openssl.cnf
make_ca ca

SUBJ_BASE="${SUBJ_BASE:-/C=US/ST=CA/O=Apache/}"

echo "Generating Root CA key:"
openssl genrsa -aes256 \
      -out ca/private/ca.key.pem \
      -passout file:pass_ca \
      4096
chmod 400 ca/private/ca.key.pem

echo "Generating Root cert:"
openssl req -config ca/openssl.cnf \
      -key ca/private/ca.key.pem \
      -passin file:pass_ca \
      -subj "${SUBJ_BASE}CN=CA" \
      -new -x509 -days 7300 -sha256 -extensions v3_ca \
      -out ca/certs/ca.cert.pem

chmod 444 ca/certs/ca.cert.pem

echo "Verify Root cert:"
openssl x509 -noout -text -in ca/certs/ca.cert.pem

echo "Making Intermediate Authority"
mkdir -p ca/intermediate
cp openssl-intermediate.cnf ca/intermediate/openssl.cnf

make_ca ca/intermediate
echo 1000 > ca/intermediate/crlnumber

echo "Generating Intermediate key:"
openssl genrsa -aes256 \
      -out ca/intermediate/private/intermediate.key.pem \
      -passout file:pass_intermediate \
      4096
chmod 400 ca/intermediate/private/intermediate.key.pem

echo "Generating Intermediate cert:"
openssl req -config ca/intermediate/openssl.cnf -new -sha256 \
      -key ca/intermediate/private/intermediate.key.pem \
      -passin file:pass_intermediate \
      -subj "${SUBJ_BASE}CN=CA intermediate" \
      -out ca/intermediate/csr/intermediate.csr.pem

echo "Signing intermediate cert:"
openssl ca -config ca/openssl.cnf -extensions v3_intermediate_ca \
      -days 3650 -notext -md sha256 \
      -passin file:pass_ca \
      -in ca/intermediate/csr/intermediate.csr.pem \
      -out ca/intermediate/certs/intermediate.cert.pem \
      -batch 

chmod 444 ca/intermediate/certs/intermediate.cert.pem

echo "Building certificate chain:"
cat ca/intermediate/certs/intermediate.cert.pem \
      ca/certs/ca.cert.pem > ca/intermediate/certs/ca-chain.cert.pem
chmod 444 ca/intermediate/certs/ca-chain.cert.pem
