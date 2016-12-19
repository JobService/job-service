package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;

public class JobTrackingWorkerFactoryProvider implements WorkerFactoryProvider {

    @Override
    public WorkerFactory getWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec) throws WorkerException {
        return new JobTrackingWorkerFactory(configSource, dataStore, codec);
    }
}
