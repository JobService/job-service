/*
 * Copyright 2016-2023 Open Text.
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
package com.hpe.caf.services.job.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class JobServiceBasicHttpsIT
{
    private String https_url;
    private SSLContext sslContext;
    private HttpClient httpClient;

    /**
     * SCMOD-3454 - FALSE POSITIVE on FORTIFY SCAN. SSL verification is disabled on purpose.
     */
    @BeforeTest
    public void setup() throws NoSuchAlgorithmException, KeyManagementException
    {
        https_url = System.getenv("webserviceurlhttps");

        // Set up a trust-all cert manager implementation
        TrustManager[] trustAllCertsManager = new TrustManager[]{
            new X509TrustManager()
            {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers()
                {
                    System.out.println("Trust All TrustManager getAcceptedIssuers() called");
                    return null;
                }

                @Override
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType)
                {
                    System.out.println("Trust All TrustManager checkClientTrusted() called");
                }

                @Override
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType)
                {
                    System.out.println("Trust All TrustManager CheckServerTrusted() called");
                }
            }
        };

        // Instantiate an SSLContext object which employs the trust-all cert manager.
        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCertsManager, new java.security.SecureRandom());

        // Set our HttpClient's Hostname Verifier to the no-op hostname verifier which turns hostname verification off.
        httpClient = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLContext(sslContext).build();
    }

    @Test
    public void basicCommunicationTest() throws IOException, URISyntaxException
    {
        final URI getRequestUrl = UriBuilder.fromUri(https_url)
            .path("partitions").path(UUID.randomUUID().toString())
            .path("jobs")
            .build();
        final HttpGet request = new HttpGet(getRequestUrl);

        System.out.println("Sending GET to url: " + getRequestUrl);
        final HttpResponse response = httpClient.execute(request);

        Assert.assertTrue(response.getStatusLine().getStatusCode() == 200);
        System.out.println("Response code: " + response.getStatusLine().getStatusCode());

        request.releaseConnection();
    }
    
    @Test
    public void basicCommunicationTest2() throws IOException, URISyntaxException
    {
        final URI getRequestUrl = UriBuilder.fromUri(https_url)
            .path("ping")
            .build();
        final HttpGet request = new HttpGet(getRequestUrl);

        System.out.println("Sending GET to url: " + getRequestUrl);
        final HttpResponse response = httpClient.execute(request);

        Assert.assertTrue(response.getStatusLine().getStatusCode() == 200);
        System.out.println("Response code: " + response.getStatusLine().getStatusCode());

        request.releaseConnection();
    }
}
