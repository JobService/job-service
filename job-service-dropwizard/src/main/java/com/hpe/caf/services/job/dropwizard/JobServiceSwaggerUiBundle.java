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

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

enum JobServiceSwaggerUiBundle implements ConfiguredBundle<Object>
{
    INSTANCE;

    // Note that if we ever decide to change this to serve the Swagger UI from the root, then we can get rid of the RedirectServlet
    // altogether, since it wouldn't be required at the root level.
    private static final String SWAGGER_UI_BASE_PATH = "/job-service-ui";

    @Override
    public void initialize(final Bootstrap<?> bootstrap)
    {
        bootstrap.addBundle(new AssetsBundle(
            "/META-INF/resources/webjars/opentext-swagger-ui-dist/2.0.0/",
            SWAGGER_UI_BASE_PATH + "/",
            "index.html",
            "swagger-ui"));

        bootstrap.addBundle(new AssetsBundle(
            "/swagger-ui-config/opentext-config.js",
            SWAGGER_UI_BASE_PATH + "/opentext-config.js",
            null,
            "swagger-ui-config"));

        bootstrap.addBundle(new AssetsBundle(
            "/com/hpe/caf/services/job/swagger.yaml",
            SWAGGER_UI_BASE_PATH + "/api-docs/swagger.yaml",
            null,
            "swagger-yaml",
            "text/x-yaml"));
    }

    @Override
    public void run(final Object configuration, final Environment environment) throws Exception
    {
        environment.servlets()
            .addServlet("swagger-ui-redirect", new RedirectServlet())
            .addMapping(SWAGGER_UI_BASE_PATH);
    }

    private final class RedirectServlet extends HttpServlet
    {
        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException
        {
            response.sendRedirect(SWAGGER_UI_BASE_PATH + "/");
        }
    }
}
