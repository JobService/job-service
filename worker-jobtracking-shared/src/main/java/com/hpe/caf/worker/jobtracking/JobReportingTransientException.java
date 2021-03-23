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
package com.hpe.caf.worker.jobtracking;

/**
 * Thrown when a transient failure occurs.
 */
public final class JobReportingTransientException extends JobReportingException {
    public JobReportingTransientException() {
        super();
    }


    /**
     * Create a JobReportingTransientException
     * @param message information explaining the exception
     */
    public JobReportingTransientException(final String message) {
        super(message);
    }


    /**
     * Create a JobReportingTransientException
     * @param message information explaining the exception
     * @param cause the original cause of this exception
     */
    public JobReportingTransientException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
