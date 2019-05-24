/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigException;
import com.hpe.caf.services.configuration.AppConfigProvider;
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
        AppConfig configProps = AppConfigProvider.getAppConfigProperties();
        Assert.assertEquals(configProps.getDatabaseURL(),"testUrl");
        Assert.assertEquals(configProps.getDatabaseUsername(),"testUserName");
        Assert.assertEquals(configProps.getDatabasePassword(),"testPassword");
    }

    @Test(expected = AppConfigException.class)
    public void testGetAppConfigPropertiesFailure_MissingDBProps () throws Exception {

        //  Set-up test database connection properties.
        HashMap<String, String> newEnv  = new HashMap<>();
        newEnv.put("CAF_DATABASE_USERNAME","testUserName");
        newEnv.put("CAF_DATABASE_PASSWORD","testPassword");
        TestUtil.setSystemEnvironmentFields(newEnv);

        //  Test expected failure call to class method because of missing properties/
        AppConfigProvider.getAppConfigProperties();
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

    public void testValidatePartitionId_Success() throws BadRequestException {
        ApiServiceUtil.validatePartitionId("something valid");
    }

    @Test(expected = BadRequestException.class)
    public void testValidatePartitionId_Null() throws BadRequestException {
        ApiServiceUtil.validatePartitionId(null);
    }

    @Test(expected = BadRequestException.class)
    public void testValidatePartitionId_Empty() throws BadRequestException {
        ApiServiceUtil.validatePartitionId("");
    }

    @Test(expected = BadRequestException.class)
    public void testValidatePartitionId_TooLong() throws BadRequestException {
        ApiServiceUtil.validatePartitionId("a very long partition ID more than forty characters");
    }

    @Test(expected = BadRequestException.class)
    public void testValidatePartitionId_InvalidChars() throws BadRequestException {
        ApiServiceUtil.validatePartitionId("not:valid");
    }

}
