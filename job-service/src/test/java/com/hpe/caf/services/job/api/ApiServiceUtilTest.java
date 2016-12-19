package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AppConfig.class})
@PowerMockIgnore("javax.management.*")
public final class ApiServiceUtilTest {

    @Test
    public void testGetAppConfigPropertiesSuccess () throws Exception {

        //  Set-up test database connection properties.
        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("CAF_DATABASE_URL","testUrl");
        newEnv.put("CAF_DATABASE_USERNAME","testUserName");
        newEnv.put("CAF_DATABASE_PASSWORD","testPassword");
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
        newEnv.put("CAF_DATABASE_USERNAME","testUserName");
        newEnv.put("CAF_DATABASE_PASSWORD","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);

        //  Test expected failure call to class method because of missing properties/
        AppConfig configProps = ApiServiceUtil.getAppConfigProperties();
    }

    @Test
    public void testIsNotNullOrEmpty_Success_True () throws Exception {

        String test = "Test";
        boolean isNotNullOrEmpty = ApiServiceUtil.isNotNullOrEmpty(test);
        Assert.assertTrue(isNotNullOrEmpty);

    }

    @Test
    public void testIsNotNullOrEmpty_Success_False () throws Exception {

        String test = "";
        boolean isNotNullOrEmpty = ApiServiceUtil.isNotNullOrEmpty(test);
        Assert.assertFalse(isNotNullOrEmpty);

    }

    @Test
    public void testContainsInvalidCharacters_Success_True_Period () throws Exception {

        String test = "Te.st";
        boolean isInvalid = ApiServiceUtil.containsInvalidCharacters(test);
        Assert.assertTrue(isInvalid);

    }

    @Test
    public void testContainsInvalidCharacters_Success_True_Asterisk () throws Exception {

        String test = "Te*t";
        boolean isInvalid = ApiServiceUtil.containsInvalidCharacters(test);
        Assert.assertTrue(isInvalid);

    }

    @Test
    public void testContainsInvalidCharacters_Success_False () throws Exception {

        String test = "Test";
        boolean isInvalid = ApiServiceUtil.containsInvalidCharacters(test);
        Assert.assertFalse(isInvalid);

    }

}
