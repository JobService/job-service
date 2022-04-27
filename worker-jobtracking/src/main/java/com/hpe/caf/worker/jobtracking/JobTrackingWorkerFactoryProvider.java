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
package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;

public class JobTrackingWorkerFactoryProvider implements WorkerFactoryProvider {

    /**
     * Returns a Worker Factory for creating workers for given tasks.
     *
     * @param configSource  the configuration source object containing configuration details
     * @param dataStore     the data store object for data storage
     * @param codec         the codec object to be used for serialization
     * @return WorkerFactory
     * @throws WorkerException  if the factory cannot be created
     */
    @Override
    public WorkerFactory getWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec) throws WorkerException {
        return new JobTrackingWorkerFactory(configSource, dataStore, codec);
    }
}
