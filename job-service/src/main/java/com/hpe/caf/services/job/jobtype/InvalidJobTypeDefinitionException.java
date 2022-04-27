/*
 * Copyright 2016-2022 Micro Focus or one of its affiliates.
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

/**
 * Indicates a job type definition is invalid, or the set of definitions is invalid as a whole.
 * This may also be thrown when processing a job according to a job type, for issues that cannot be
 * determined at load-time.
 */
public class InvalidJobTypeDefinitionException extends Exception {

    public InvalidJobTypeDefinitionException(final String message) {
        super(message);
    }

    public InvalidJobTypeDefinitionException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
