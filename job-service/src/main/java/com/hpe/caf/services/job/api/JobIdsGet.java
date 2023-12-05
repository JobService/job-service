package com.hpe.caf.services.job.api;

import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.job.api.filter.RsqlToSqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JobIdsGet {

    private static final Logger LOG = LoggerFactory.getLogger(JobIdsGet.class);

    /**
     * Gets a list of Job IDs from the job database specified by filter
     *
     * @param partitionId   required partitionId of the jobs to get
     * @param filter    filter to use when returning results
     */
    public static List<String> getJobIds(final String partitionId, final String filter) throws Exception
    {
        try {
            LOG.debug("getJobIds: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

            final String filterQuery = RsqlToSqlUtils.convertToSqlSyntax(filter);

            // Get app config settings
            LOG.debug("getJobs: Reading database connection properties...");
            AppConfig config = AppConfigProvider.getAppConfigProperties();

            final DatabaseHelper databaseHelper = new DatabaseHelper(config);

            LOG.debug("getJobIds: Done.");
            return databaseHelper.getJobIds(partitionId, filter);

        } catch (Exception e) {
            LOG.error("Error - ", e);
            throw e;
        }
    }
}
