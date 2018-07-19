#!/bin/bash 

/usr/hdp/current/knox-server/bin/gateway.sh stop

/usr/hdp/current/knox-server/bin/knoxcli.sh create-master --force --value $KNOX_PASSWORD

rm /usr/hdp/current/knox-server/data/security/keystores/*
cp /root/gateway.jks /usr/hdp/current/knox-server/data/security/keystores/

/usr/hdp/current/knox-server/bin/knoxcli.sh create-alias gateway-identity-passphrase --value $KNOX_PASSWORD

