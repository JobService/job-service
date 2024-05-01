package com.hpe.caf.services.job.scheduled.executor;

public final class StagingQueueRerouter {
    private StagingQueueRerouter() {};

    private static final String LOAD_BALANCED_INDICATOR = "»";

    /**
     * Create a staging queue name from the 
     * @param targetQueue The original queue name
     * @param tenant The tenant id
     * @return A new queue name that combines the original queue name with the tenant id
     */
    public static String route(final String targetQueue, final String tenant) {
        return targetQueue + LOAD_BALANCED_INDICATOR + "/" + tenant;
    }
}
