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
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class JobServiceBasicHttpsIT
{
    private String https_url;
    private SSLContext sslContext;
    private HttpClient httpClient;

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
    public void basicCommunicationTest() throws IOException
    {
        final String getRequestUrl = https_url + "/jobs";
        final HttpGet request = new HttpGet(getRequestUrl);

        System.out.println("Sending GET to url: " + getRequestUrl);
        final HttpResponse response = httpClient.execute(request);

        Assert.assertTrue(response.getStatusLine().getStatusCode() == 200);
        System.out.println("Response code: " + response.getStatusLine().getStatusCode());

        request.releaseConnection();
    }
}
