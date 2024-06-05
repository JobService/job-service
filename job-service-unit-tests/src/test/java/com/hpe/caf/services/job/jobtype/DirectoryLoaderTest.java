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
package com.hpe.caf.services.job.jobtype;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectoryLoaderTest {
    // record of calls made to `parser` - map from job type ID to stream contents
    private Map<String, String> parserCalls;
    private DefinitionParser parser;

    private static final class JobTypeComparator implements Comparator<JobType> {
        @Override
        public int compare(final JobType t1, final JobType t2) {
            return t1.getId().compareTo(t2.getId());
        }
    }

    @TempDir
    public File tempFiles;

    @BeforeEach
    public final void setUp() {
        parserCalls = new HashMap<>();
        parser = (id, stream) -> {
            try {
                final String contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                parserCalls.put(id, contents);
                if ("1".equals(contents)) return JobTypeTestUtil.testJobType1;
                else if ("2".equals(contents)) return JobTypeTestUtil.testJobType2;
                else throw new InvalidJobTypeDefinitionException(contents);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Test
    public void testLoadEmptyDir() throws Exception {
        final Path dir = tempFiles.toPath();
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        assertEquals(Collections.EMPTY_LIST, definitions);
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testLoadNonExistentDir() throws Exception {
        final Path dir = tempFiles.toPath().resolve("missing");
        Assertions.assertThrows(NoSuchFileException.class, () -> new DirectoryLoader(parser, dir).load());
    }

    @Test
    public void testLoadWithDefinitions() throws Exception {
        final Path dir = tempFiles.toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("defn B.yaml"), "2".getBytes(StandardCharsets.UTF_8));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();

        final Map<String, String> expectedParserCalls = new HashMap<>();
        expectedParserCalls.put("defn A", "1");
        expectedParserCalls.put("defn B", "2");
        assertEquals(expectedParserCalls, parserCalls, "should extract the job type IDs from the filenames");

        definitions.sort(new JobTypeComparator());
        assertEquals(Arrays.asList(JobTypeTestUtil.testJobType1, JobTypeTestUtil.testJobType2), definitions, "should return the loaded job types");
    }

    @Test
    public void testLoadWithSubdirectory() throws Exception {
        final Path dir = tempFiles.toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(dir.resolve("defn B.yaml"));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        assertEquals(Collections.singletonList(JobTypeTestUtil.testJobType1), definitions);
    }

    @Test
    public void testLoadWithFileWithWrongExtension() throws Exception {
        final Path dir = tempFiles.toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("defn B.json"), "2".getBytes(StandardCharsets.UTF_8));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        assertEquals(Collections.singletonList(JobTypeTestUtil.testJobType1), definitions);
    }

    @Test
    public void testLoadWithFileWithNoExtension() throws Exception {
        final Path dir = tempFiles.toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("defn B"), "2".getBytes(StandardCharsets.UTF_8));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        assertEquals(Collections.singletonList(JobTypeTestUtil.testJobType1), definitions);
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testLoadWithInvalidDefinition() throws Exception {
        final Path dir = tempFiles.toPath();
        // parser implementation treats '3' as an invalid definition
        Files.write(dir.resolve("defn.yaml"), "3".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> new DirectoryLoader(parser, dir).load());
    }

}
