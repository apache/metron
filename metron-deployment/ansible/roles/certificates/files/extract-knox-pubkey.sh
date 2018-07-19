#!/bin/bash
cat /root/$1/$1.cert.pem | grep -v 'CERTIFICATE' | paste -sd "" -