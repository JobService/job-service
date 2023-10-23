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
package com.hpe.caf.services.job.jobtype;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.io.IOException;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads the taskScript schema on application startup and initialises the JsonSchemaTaskScriptValidator singleton with it.
 */
public final class TaskScriptSchemaContextListener implements ServletContextListener
{
    private static final String TASK_SCRIPT_SCHEMA_NAME = "com/hpe/caf/services/job/jobtype/taskscript-schema.yaml";

    @Override
    public void contextInitialized(final ServletContextEvent event)
    {
        try (final InputStream schemaAsInputStream = getClass().getClassLoader().getResourceAsStream(TASK_SCRIPT_SCHEMA_NAME)){
            final Object schemaAsObject = new Yaml().load(schemaAsInputStream);
            final JsonNode schemaAsJsonNode = new ObjectMapper().convertValue(schemaAsObject, JsonNode.class);
            JsonSchemaTaskScriptValidator.initialise(schemaAsJsonNode);
        } catch (final IllegalArgumentException | IOException e) {
            // note: this doesn't cause the application to shut down
            throw new RuntimeException("Error loading taskScript schema", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event)
    {
    }
}
