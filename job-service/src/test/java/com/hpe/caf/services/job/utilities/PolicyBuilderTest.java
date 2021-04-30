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
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.Policer;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.testng.Assert.*;

public class PolicyBuilderTest {

    final NewJob job = new NewJob();
    final ExpirablePolicy systemDefaultExpirablePolicy = new ExpirablePolicy();
    final DeletePolicy systemDefaultDeletePolicy = new DeletePolicy();

    @Before
    public void init(){
        systemDefaultExpirablePolicy.setOperation(ExpirablePolicy.OperationEnum.EXPIRE);
        systemDefaultExpirablePolicy.setExpiryTime("createTime+P90M");
        systemDefaultExpirablePolicy.setPolicer(Policer.System);
        systemDefaultDeletePolicy.setOperation(OperationEnum.DELETE);
        systemDefaultDeletePolicy.setExpiryTime("none");
        systemDefaultDeletePolicy.setPolicer(Policer.System);
    }

    @Test
    public void checkPolicer() throws BadRequestException {
        final ExpirationPolicy expirationPolicy = new ExpirationPolicy();
        final ExpirablePolicy activeExpirablePolicy = new ExpirablePolicy();
        activeExpirablePolicy.setExpiryTime("none");
        activeExpirablePolicy.setOperation(ExpirablePolicy.OperationEnum.DELETE);
        expirationPolicy.setActive(activeExpirablePolicy);

        final DeletePolicy completedPolicy = new DeletePolicy();
        completedPolicy.setExpiryTime("createTime+PT1H");
        completedPolicy.setOperation(OperationEnum.DELETE);
        expirationPolicy.setCompleted(completedPolicy);
        job.setExpiry(expirationPolicy);
        PolicyBuilder.buildPolicyMap(job);

        assertAll(
                ()-> assertEquals(expirationPolicy.getActive().getPolicer(), Policer.User),
                ()-> assertEquals(expirationPolicy.getCompleted().getPolicer(), Policer.User),
                ()-> assertEquals(expirationPolicy.getCancelled().getPolicer(), Policer.System),
                ()-> assertEquals(expirationPolicy.getExpired().getPolicer(), Policer.System),
                ()-> assertEquals(expirationPolicy.getPaused().getPolicer(), Policer.System),
                ()-> assertEquals(expirationPolicy.getWaiting().getPolicer(), Policer.System),
                ()-> assertEquals(expirationPolicy.getFailed().getPolicer(), Policer.System)
        );
    }

    @Test
    public void fillUserDefaultPolicyWhenMissing() throws BadRequestException {
        // Set partial policy
        final ExpirationPolicy expirationPolicy = new ExpirationPolicy();
        final ExpirablePolicy activeExpirablePolicy = new ExpirablePolicy();
        activeExpirablePolicy.setExpiryTime("none");
        activeExpirablePolicy.setOperation(ExpirablePolicy.OperationEnum.DELETE);
        expirationPolicy.setActive(activeExpirablePolicy);
        final DeletePolicy completedPolicy = new DeletePolicy();
        completedPolicy.setExpiryTime("createTime+PT1H");
        completedPolicy.setOperation(OperationEnum.DELETE);
        expirationPolicy.setCompleted(completedPolicy);
        job.setExpiry(expirationPolicy);

        PolicyBuilder.buildPolicyMap(job);

        assertAll(
                ()-> assertEquals(job.getExpiry().getActive(), activeExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getCompleted(), completedPolicy),
                ()-> assertEquals(job.getExpiry().getPaused(), systemDefaultExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getWaiting(), systemDefaultExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getExpired(), systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getCancelled(), systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getFailed(), systemDefaultDeletePolicy)
        );
    }

    @Test
    public void fillSystemDefaultPolicyWhenMissing() throws BadRequestException {
        // Set partial policy
        final ExpirationPolicy expirationPolicy = new ExpirationPolicy();
        final ExpirablePolicy activeExpirablePolicy = new ExpirablePolicy();
        activeExpirablePolicy.setExpiryTime("none");
        activeExpirablePolicy.setOperation(ExpirablePolicy.OperationEnum.DELETE);
        expirationPolicy.setActive(activeExpirablePolicy);
        final DeletePolicy completedPolicy = new DeletePolicy();
        completedPolicy.setExpiryTime("createTime+PT1H");
        completedPolicy.setOperation(OperationEnum.DELETE);
        expirationPolicy.setCompleted(completedPolicy);
        job.setExpiry(expirationPolicy);

        PolicyBuilder.buildPolicyMap(job);

        assertAll(
                ()-> assertEquals(job.getExpiry().getActive(), activeExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getCompleted(), completedPolicy),
                ()-> assertEquals(job.getExpiry().getPaused(), systemDefaultExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getWaiting(), systemDefaultExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getExpired(), systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getCancelled(), systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getFailed(), systemDefaultDeletePolicy)
        );
    }

    @Test
    public void fillDefaultPolicyWhenEmpty() throws BadRequestException {

        PolicyBuilder.buildPolicyMap(job);

        assertAll(
                ()-> assertEquals(job.getExpiry().getActive() ,systemDefaultExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getCancelled() ,systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getCompleted() ,systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getExpired() ,systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getFailed() ,systemDefaultDeletePolicy),
                ()-> assertEquals(job.getExpiry().getPaused() ,systemDefaultExpirablePolicy),
                ()-> assertEquals(job.getExpiry().getWaiting() ,systemDefaultExpirablePolicy)
        );
    }
}
