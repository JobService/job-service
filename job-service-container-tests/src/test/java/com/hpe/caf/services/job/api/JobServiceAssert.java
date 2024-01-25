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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.client.ApiException;
import org.testng.Assert;

import jakarta.ws.rs.core.Response;

public class JobServiceAssert {

    /**
     * Assert that an API call fails with a specific error.
     *
     * @param status HTTP status code to expect
     * @param apiCall Function which calls the API
     */
    public static void assertThrowsApiException(
        final Response.Status status, final MaybeFail apiCall
    ) {
        ApiException apiErr = null;
        try {
            apiCall.run();
        } catch (ApiException e) {
            apiErr = e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotNull(apiErr, "API call should fail");
        Assert.assertEquals(apiErr.getCode(), status.getStatusCode(),
            "error code should be " + status);
    }


    /**
     * A thread which captures any exception thrown, to help make assertions in threads.
     */
    public static class TestThread extends Thread {
        private final MaybeFail run;
        private final Throwable[] thrown = new Throwable[] { null };

        /**
         * @param f The function to run in another thread
         */
        public TestThread(final MaybeFail run) {
            this.run = run;
        }

        @Override
        public void run() {
            super.run();
            try {
                run.run();
            } catch (Throwable e) {
                thrown[0] = e;
            }
        }

        /**
         * Call this after the thread has finished to retrieve any exception that was thrown in the
         * thread (possibly `null`).
         */
        public Throwable getThrown() {
            return thrown[0];
        }

        /**
         * Call this after the thread has finished to throw any exception that was thrown in the
         * thread.
         */
        public void handleThrown() throws Throwable {
            if (getThrown() != null) {
                throw getThrown();
            }
        }

    }


    /**
     * A function that might throw.
     */
    @FunctionalInterface
    public interface MaybeFail {
        void run() throws Exception;
    }

}
