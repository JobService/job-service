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

import com.hpe.caf.services.job.api.generated.model.DeletePolicy.ExpirationOperationEnum;
import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import com.hpe.caf.services.job.api.generated.model.Policy;

import java.util.ArrayList;
import java.util.List;

public class ExpirationPolicyHelper {

    private final ExpirationPolicy expirationPolicy;

    public ExpirationPolicyHelper(ExpirationPolicy expirationPolicy) {
        this.expirationPolicy = expirationPolicy;
    }


    public List<String> toDBString() {
        final List<String> policyList = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        buildPolicy(policyList, sb, "Active,", this.expirationPolicy.getActive());
        buildPolicy(policyList, sb, "Cancelled,", this.expirationPolicy.getCancelled());
        buildPolicy(policyList, sb, "Completed,", this.expirationPolicy.getCompleted());
        buildPolicy(policyList, sb, "Failed,", this.expirationPolicy.getFailed());
        buildPolicy(policyList, sb, "Waiting,", this.expirationPolicy.getWaiting());
        buildPolicy(policyList, sb, "Paused,", this.expirationPolicy.getPaused());
        sb.setLength(0);
        sb.append("(,,");
        sb.append("Expired,")
                .append(ExpirationOperationEnum.DELETE.toString()).append(",")
                .append(this.expirationPolicy.getExpired().getExpiryTime())
                .append(")");
        policyList.add(sb.toString());

        return policyList;
    }

    private void buildPolicy(final List<String> policyList, final StringBuilder sb, final String status, final Policy policy) {
        sb.setLength(0);
        sb.append("(,,");
        sb.append(status)
                .append(policy.getOperation().toString()).append(",")
                .append(policy.getExpiryTime())
                .append(")");
        policyList.add(sb.toString());
    }
}
