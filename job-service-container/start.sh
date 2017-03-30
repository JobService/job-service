#!/bin/bash
# Check if the SSL_TOMCAT_CA_CERT_LOCATION environment variable is set, if so, run the setup-tomcat-ssl-cert.sh script.
if [ -n "${SSL_TOMCAT_CA_CERT_LOCATION}" ]
then
    echo "Tomcat CA Cert provided at location: ${SSL_TOMCAT_CA_CERT_LOCATION}"
    /container-cert-script/setup-tomcat-ssl-cert.sh
fi

$CATALINA_HOME/bin/catalina.sh run