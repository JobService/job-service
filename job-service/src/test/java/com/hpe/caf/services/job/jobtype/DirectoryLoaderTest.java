/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

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

    @Rule
    public final TemporaryFolder tempFiles = new TemporaryFolder();

    @Before
    public final void setUp() {
        parserCalls = new HashMap<>();
        parser = (id, stream) -> {
            try {
                final String contents = IOUtils.toString(stream);
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
        final Path dir = tempFiles.newFolder().toPath();
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        Assert.assertEquals(Collections.EMPTY_LIST, definitions);
    }

    @Test(expected = NoSuchFileException.class)
    public void testLoadNonExistentDir() throws Exception {
        final Path dir = tempFiles.newFolder().toPath().resolve("missing");
        new DirectoryLoader(parser, dir).load();
    }

    @Test
    public void testLoadWithDefinitions() throws Exception {
        final Path dir = tempFiles.newFolder().toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("defn B.yaml"), "2".getBytes(StandardCharsets.UTF_8));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();

        final Map<String, String> expectedParserCalls = new HashMap<>();
        expectedParserCalls.put("defn A", "1");
        expectedParserCalls.put("defn B", "2");
        Assert.assertEquals("should extract the job type IDs from the filenames",
            expectedParserCalls, parserCalls);

        definitions.sort(new JobTypeComparator());
        Assert.assertEquals("should return the loaded job types",
            Arrays.asList(JobTypeTestUtil.testJobType1, JobTypeTestUtil.testJobType2),
            definitions);
    }

    @Test
    public void testLoadWithSubdirectory() throws Exception {
        final Path dir = tempFiles.newFolder().toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(dir.resolve("defn B.yaml"));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        Assert.assertEquals(Collections.singletonList(JobTypeTestUtil.testJobType1), definitions);
    }

    @Test
    public void testLoadWithFileWithWrongExtension() throws Exception {
        final Path dir = tempFiles.newFolder().toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("defn B.json"), "2".getBytes(StandardCharsets.UTF_8));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        Assert.assertEquals(Collections.singletonList(JobTypeTestUtil.testJobType1), definitions);
    }

    @Test
    public void testLoadWithFileWithNoExtension() throws Exception {
        final Path dir = tempFiles.newFolder().toPath();
        Files.write(dir.resolve("defn A.yaml"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("defn B"), "2".getBytes(StandardCharsets.UTF_8));
        final List<JobType> definitions = new DirectoryLoader(parser, dir).load();
        Assert.assertEquals(Collections.singletonList(JobTypeTestUtil.testJobType1), definitions);
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testLoadWithInvalidDefinition() throws Exception {
        final Path dir = tempFiles.newFolder().toPath();
        // parser implementation treats '3' as an invalid definition
        Files.write(dir.resolve("defn.yaml"), "3".getBytes(StandardCharsets.UTF_8));
        new DirectoryLoader(parser, dir).load();
    }

}
