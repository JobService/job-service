/*
 * Copyright 2016-2024 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.services.job.dropwizard;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import java.util.ArrayList;
import java.util.List;

enum DropWizardSslBundle implements ConfiguredBundle<Configuration>
{
    INSTANCE;

    private static final String SSL_KEYSTORE_PATH = System.getenv("SSL_KEYSTORE_PATH");
    private static final String SSL_KEYSTORE = System.getenv("SSL_KEYSTORE");
    private static final String SSL_KEYSTORE_PASSWORD = System.getenv("SSL_KEYSTORE_PASSWORD");
    private static final String SSL_CERT_ALIAS = System.getenv("SSL_CERT_ALIAS");
    private static final String SSL_KEYSTORE_TYPE = System.getenv("SSL_KEYSTORE_TYPE");
    private static final String SSL_VALIDATE_CERTS = System.getenv("SSL_VALIDATE_CERTS");
    private static final String SSL_DISABLE_SNI_HOST_CHECK = System.getenv("SSL_DISABLE_SNI_HOST_CHECK");
    private static final String HTTPS_PORT = System.getenv("HTTPS_PORT");

    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception
    {
        if (!isHttpsEnabled()) {
            return;
        }

        final HttpsConnectorFactory httpsConnectorFactory = new HttpsConnectorFactory();

        httpsConnectorFactory.setPort(isNotNullOrEmpty(HTTPS_PORT) ? Integer.parseInt(HTTPS_PORT) : 8443);
        httpsConnectorFactory.setKeyStorePath(SSL_KEYSTORE_PATH + "/" + SSL_KEYSTORE);
        httpsConnectorFactory.setKeyStorePassword(SSL_KEYSTORE_PASSWORD);
        httpsConnectorFactory.setKeyStoreType(isNotNullOrEmpty(SSL_KEYSTORE_TYPE) ? SSL_KEYSTORE_TYPE : "JKS");
        httpsConnectorFactory.setCertAlias(SSL_CERT_ALIAS);
        httpsConnectorFactory.setValidateCerts(
            isNotNullOrEmpty(SSL_VALIDATE_CERTS)
            && Boolean.parseBoolean(SSL_VALIDATE_CERTS));
        httpsConnectorFactory.setDisableSniHostCheck(
            isNotNullOrEmpty(SSL_DISABLE_SNI_HOST_CHECK)
            && Boolean.parseBoolean(SSL_DISABLE_SNI_HOST_CHECK));

        final DefaultServerFactory serverFactory = (DefaultServerFactory) configuration.getServerFactory();
        final List<ConnectorFactory> applicationConnectors = serverFactory.getApplicationConnectors();
        try {
            applicationConnectors.add(httpsConnectorFactory);
        } catch (final UnsupportedOperationException ex) {
            final List<ConnectorFactory> newApplicationConnectors = new ArrayList<>(applicationConnectors);
            newApplicationConnectors.add(httpsConnectorFactory);
            serverFactory.setApplicationConnectors(newApplicationConnectors);
        }
    }

    private static boolean isHttpsEnabled()
    {
        return isNotNullOrEmpty(SSL_KEYSTORE_PATH)
            && isNotNullOrEmpty(SSL_KEYSTORE)
            && isNotNullOrEmpty(SSL_KEYSTORE_PASSWORD)
            && isNotNullOrEmpty(SSL_CERT_ALIAS);
    }

    private static boolean isNotNullOrEmpty(final String value)
    {
        return value != null && !value.isEmpty();
    }
}
