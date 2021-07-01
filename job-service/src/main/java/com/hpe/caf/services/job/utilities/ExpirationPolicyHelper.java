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
package com.hpe.caf.services.job.utilities;

import com.hpe.caf.services.job.api.generated.model.DeletePolicy;
import com.hpe.caf.services.job.api.generated.model.DeletePolicy.OperationEnum;
import com.hpe.caf.services.job.api.generated.model.ExpirablePolicy;
import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ExpirationPolicyHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(ExpirationPolicyHelper.class);
    private ExpirationPolicyHelper()
    {
    }

    public static List<String> toPgCompositeList(final ExpirationPolicy expirationPolicy)
    {
        LOG.debug("converting into composite");
        final List<String> policyList = new ArrayList<>();
        // build policy when status could be set to expire
        if(null!= expirationPolicy.getActive())buildExpirablePolicy(policyList, "Active", expirationPolicy.getActive());
        if(null!= expirationPolicy.getWaiting())buildExpirablePolicy(policyList, "Waiting", expirationPolicy.getWaiting());
        if(null!= expirationPolicy.getPaused())buildExpirablePolicy(policyList, "Paused", expirationPolicy.getPaused());

        // build policy when delete is the only option
        if(null!= expirationPolicy.getCancelled())buildDeletePolicy(policyList, "Cancelled", expirationPolicy.getCancelled());
        if(null!= expirationPolicy.getCompleted())buildDeletePolicy(policyList, "Completed", expirationPolicy.getCompleted());
        if(null!= expirationPolicy.getFailed())buildDeletePolicy(policyList, "Failed", expirationPolicy.getFailed());
        if(null!= expirationPolicy.getExpired())buildDeletePolicy(policyList, "Expired", expirationPolicy.getExpired());
        LOG.debug("policyList {}", policyList);
        return policyList;
    }

    private static void buildDeletePolicy(final List<String> policyList, final String status, final DeletePolicy deletePolicy) {
        policyList.add(toJobPolicyDbTypeString(
                status,
                OperationEnum.DELETE,
                deletePolicy.getExpiryTime()));
    }

    private static void buildExpirablePolicy(final List<String> policyList, final String status, final ExpirablePolicy expirablePolicy)
    {
        policyList.add(toJobPolicyDbTypeString(status, expirablePolicy));
    }

    private static String toJobPolicyDbTypeString(final String status, final ExpirablePolicy expirablePolicy)
    {
        return toJobPolicyDbTypeString(status, expirablePolicy.getOperation(), expirablePolicy.getExpiryTime());
    }

    private static String toJobPolicyDbTypeString(final String status, final Object operation, final String expiryTime)
    {
        // Builds up the Composite Value for the JOB_POLICY database type
        // See https://www.postgresql.org/docs/current/rowtypes.html

        // The expiry time has already been validated.
        // The allowed patterns do not contain any commas or parentheses so there is no need for escaping here.
        //
        // The definition of JOB_POLICY is (partition_id, job_id, job_status, operation, expiration_time)
        return "(,,(\"" + status + "," + operation + "," + expiryTime + ")\")";
    }
}
