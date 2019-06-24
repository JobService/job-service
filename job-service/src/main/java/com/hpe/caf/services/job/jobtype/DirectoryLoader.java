/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Load job types from definition files in a directory.  Includes only files directly in the
 * directory with names ending in '.yaml'.
 */
public final class DirectoryLoader implements Loader {
    private final DefinitionParser parser;
    /**
     * Directory containing definitions.
     */
    private final Path dir;

    /**
     * @param parser Used to parse loaded definitions
     * @param dir Directory to look for definition files in
     */
    public DirectoryLoader(final DefinitionParser parser, final Path dir) {
        this.parser = parser;
        this.dir = dir;
    }

    @Override
    public List<JobType> load() throws IOException, InvalidJobTypeDefinitionException {
        final List<JobType> defns = new ArrayList<>();
        for (final Path file : (Iterable<Path>) Files.list(dir)::iterator) {
            if (Files.isRegularFile(file) && file.toString().endsWith(".yaml")) {
                try (final InputStream defnStream = Files.newInputStream(file)) {
                    defns.add(parser.parse(defnStream));
                }
            }
        }
        return defns;
    }

}
