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

import com.codahale.metrics.health.HealthCheckRegistry;
import com.github.cafapi.correlation.dropwizard.CorrelationIdBundle;
import com.github.cafapi.ssl.dropwizard.DropWizardSslBundleProvider;
import com.hpe.caf.services.job.api.JobServiceModule;
import com.hpe.caf.services.job.dropwizard.health.DatabaseHealthCheck;
import com.hpe.caf.services.job.dropwizard.health.PingHealthCheck;
import com.hpe.caf.services.job.dropwizard.health.QueueHealthCheck;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.logging.common.LoggingUtil;

public final class JobServiceApplication extends Application<JobServiceConfiguration>
{
    private final boolean useInternalConfig;

    private JobServiceApplication(final boolean useInternalConfig)
    {
        this.useInternalConfig = useInternalConfig;
    }

    public static void main(final String[] args) throws Exception
    {
        if (args.length == 0) {
            new JobServiceApplication(true).run("server", "/config.yaml");
        } else {
            new JobServiceApplication(false).run(args);
        }
    }

    @Override
    protected void bootstrapLogging()
    {
        LoggingUtil.hijackJDKLogging();
    }

    @Override
    public void initialize(final Bootstrap<JobServiceConfiguration> bootstrap)
    {
        // Pick up the built-in config file from resources
        if (useInternalConfig) {
            bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                    new ResourceConfigurationSourceProvider(),
                    new EnvironmentVariableSubstitutor(false, true)));
        }

        // Add functionality bundles
        bootstrap.addBundle(new CorrelationIdBundle<>());
        bootstrap.addBundle(DropWizardSslBundleProvider.getInstance());
        bootstrap.addBundle(JobServiceSwaggerUiBundle.INSTANCE);
    }

    @Override
    public void run(
        final JobServiceConfiguration configuration,
        final Environment environment
    ) throws Exception
    {
        final HealthCheckRegistry healthChecks = environment.healthChecks();
        healthChecks.register("database", new DatabaseHealthCheck());
        healthChecks.register("ping", new PingHealthCheck());
        healthChecks.register("queue", new QueueHealthCheck());

        JobServiceModule.registerProviders(environment.jersey()::register);
    }
}
