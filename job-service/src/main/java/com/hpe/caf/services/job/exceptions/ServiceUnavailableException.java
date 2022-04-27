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
package com.hpe.caf.services.job.exceptions;

/**
 * Custom exception implemented for the job service api. Exceptions of this type map directly onto http 503 status codes.
 */
public final class ServiceUnavailableException extends Exception
{
    public ServiceUnavailableException(final String message)
    {
        super(message);
    }

    public ServiceUnavailableException(final String message, final Throwable throwable)
    {
        super(message, throwable);
    }
}
