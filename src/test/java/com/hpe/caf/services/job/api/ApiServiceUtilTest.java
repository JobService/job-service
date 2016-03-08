package com.hpe.caf.services.job.api;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class ApiServiceUtilTest {

    @Test
    public void testGetAppConfigPropertiesSuccess () throws Exception {

        //  Set-up test database connection properties.
        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("database.url","testUrl");
        newEnv.put("database.username","testUserName");
        newEnv.put("database.password","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);

        //  Test successful call to class method.
        AppConfig configProps = ApiServiceUtil.getAppConfigProperties();
        Assert.assertEquals(configProps.getDatabaseURL(),"testUrl");
        Assert.assertEquals(configProps.getDatabaseUsername(),"testUserName");
        Assert.assertEquals(configProps.getDatabasePassword(),"testPassword");
    }

    @Test(expected = BadRequestException.class)
    public void testGetAppConfigPropertiesFailure_MissingDBProps () throws Exception {

        //  Set-up test database connection properties.
        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("database.username","testUserName");
        newEnv.put("database.password","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);

        //  Test expected failure call to class method because of missing properties/
        AppConfig configProps = ApiServiceUtil.getAppConfigProperties();
    }

}
