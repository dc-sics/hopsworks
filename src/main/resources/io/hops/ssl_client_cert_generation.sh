#!/bin/bash

if [ $# -ne 4  ]; then
 echo "incorrect usage"
 exit 1
fi

param1=$1
param1=$2

PASSWORD=test1234
VALIDITY=365


keytool -keystore kafka.server.keystore.jks -alias localhost -validity $VALIDITY -genkey
openssl req -new -x509 -keyout ca-key -out ca-cert -days $VALIDITY
keytool -keystore kafka.server.truststore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.client.truststore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.server.keystore.jks -alias localhost -certreq -file cert-file
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days $VALIDITY -CAcreateserial -passin pass:$PASSWORD
keytool -keystore kafka.server.keystore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.server.keystore.jks -alias localhost -import -file cert-signed keytool -keystore kafka.client.keystore.jks -alias localhost -validity $VALIDITY -genkey
keytool -keystore kafka.client.keystore.jks -alias localhost -certreq -file cert-file
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days $VALIDITY -CAcreateserial -passin pass:$PASSWORD
keytool -keystore kafka.client.keystore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.client.keystore.jks -alias localhost -import -file cert-signed

