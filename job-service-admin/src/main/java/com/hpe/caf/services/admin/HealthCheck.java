/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.services.admin;

//import com.google.gson.Gson;
//import com.hpe.darwin.tag.client.ApiClient;
//import com.hpe.darwin.tag.client.ApiException;
//import com.hpe.darwin.tag.client.api.StatusApi;
//import com.hpe.darwin.tag.client.model.StatusResponse;
//import com.hpe.darwin.tag.service.common.loggers.LogManager;
//import com.hpe.darwin.tag.service.common.loggers.Logger;

import com.google.gson.Gson;
import com.hpe.caf.services.job.configuration.AppConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class HealthCheck extends HttpServlet
{
    private SessionFactory sessionFactory;
    private static Logger LOG = LoggerFactory.getLogger(HealthCheck.class);

    private static final String CAF_TAG_WEBSERVICE_URL = "CAF_TAG_WEBSERVICE_URL";

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse res) throws IOException
    {
        LOG.debug("Database Health Check: Starting...");

        //  Construct response payload.
        final Map<String, String> statusResponseMap = new HashMap<>();
        // Health check that the DB can be contacted
        performDBHealthCheck(statusResponseMap);


        // TODO: Health check that RabbitMQ can be contacted
            // TODO: Build URL for RabbitMQ
            // TODO: Check how other services healthcheck the RabbitMQ endpoint

        // TODO: Health check that the GetJobs API operation returns a 200 code
            // TODO: Build URL for the API
            // TODO: Check how the integration tests communicate with the API



        //  Get handle a to the Tag Service Api client.
//        LOGGER.debug("Getting handle to the Api client ...");
//        final StatusApi statusApi = new StatusApi(getApiClient());
//
//        final StatusResponse statusResponse;
//        int statusCode = 200;   // Assume (200) indicating the request succeeded normally.
//        String responseBody;
//
//        try {
//            //  Use the Api client to return the service status.
//            LOGGER.debug("Checking CAF Tag Service status ...");
//            statusResponse = statusApi.getTagServiceStatus();
//
//            //  Construct response payload.
//            final Map<String, Map<String, String>> statusResponseMap = new HashMap<>();
//            for (final Map.Entry e : statusResponse.entrySet()) {
//                statusResponseMap.put(e.getKey().toString(), (Map<String, String>) e.getValue());
//            }
//            final Gson gson = new Gson();
//            responseBody = gson.toJson(statusResponseMap);
//
//        } catch (final ApiException e) {
//            //  The swagger generated code will throw ApiExceptions for tag service 500 and 503 responses.
//            //  Need to capture the relevant status code and response body from the exception details and
//            //  construct appropriate response from these.
//            LOGGER.error(MessageFormat.format("ApiException thrown when checking CAF Tag Service status. {0}", e.getMessage()));
//            LOGGER.debug("Capturing status code and response body from the ApiException details ...");
//            statusCode = e.getCode();
//            responseBody = e.getResponseBody();
//        }

        final Gson gson = new Gson();
        final String responseBody = gson.toJson(statusResponseMap);

        //  Get response body bytes.
        final byte[] responseBodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);

        //  Set content type and length.
        res.setContentType("application/json");
        res.setContentLength(responseBodyBytes.length);

        //  Add CacheControl header to specify directives for caching mechanisms.
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        //  Set status code.
        res.setStatus(statusCode);

        //  Output response body.
        try (ServletOutputStream out = res.getOutputStream())
        {
            out.write(responseBodyBytes);
            out.flush();
        }

    }

    private void performDBHealthCheck(Map<String, String> statusResponseMap) {
        sessionFactory = HibernateUtil.getSessionFactory(new AppConfig());
        final Session session = sessionFactory.openSession();

        try {
            session.doWork(new Work()
            {
                public void execute(Connection connection) throws SQLException
                {
                    LOG.debug("Database Health Check: Attempting to Contact Database");
                    Statement stmt = connection.createStatement();
                    stmt.execute("SELECT 1");
                    LOG.debug("Database Health Check: Connection to Database Achieved");
                }
            });
            LOG.debug("Database Health Check: Healthy");
            statusResponseMap.put("DB Connection", "Healthy");
        } catch (Exception e) {
            LOG.error("Database Health Check: Unhealthy : " + e.toString());
            statusResponseMap.put("DB Connection", "Unhealthy");
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

//    private static ApiClient getApiClient()
//    {
//        //  Create new instance of the Api client.
//        final ApiClient apiClient = new ApiClient();
//
//        //  Set base path on which the Tag Service API is served. This is relative to the host.
//        LOGGER.debug(CAF_TAG_WEBSERVICE_URL + ": " + System.getenv(CAF_TAG_WEBSERVICE_URL));
//        String cafTagWebserviceUrl = System.getenv(CAF_TAG_WEBSERVICE_URL);
//        if (null == cafTagWebserviceUrl || cafTagWebserviceUrl.isEmpty()) {
//            cafTagWebserviceUrl = "http://localhost:8080/tag-service/v1";
//        }
//        apiClient.setBasePath(cafTagWebserviceUrl);
//
//        //  Set the date format used to parse/format date parameters.
//        apiClient.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
//
//        return apiClient;
//    }

}
