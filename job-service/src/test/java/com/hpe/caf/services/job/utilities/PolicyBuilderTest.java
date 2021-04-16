package com.hpe.caf.services.job.utilities;

import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import junit.framework.TestCase;
import org.junit.Test;

public class PolicyBuilderTest
        extends TestCase {

    final NewJob job = new NewJob();

    @Test
    public void testBuildPolicyMap() throws BadRequestException {
        PolicyBuilder.buildPolicyMap(job);
        System.out.println("job: "+job);
    }
}
