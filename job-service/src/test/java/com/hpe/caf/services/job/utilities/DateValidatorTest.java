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

import junit.framework.TestCase;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static com.hpe.caf.services.job.utilities.DateValidator.validate;
import static org.testng.Assert.assertThrows;

public class DateValidatorTest
        extends TestCase {

    @Test
    public void testInvalidDate(){
        assertAll(
                ()-> assertThrows(Exception.class, ()-> validate("202dsds20:50.52Z")),
                ()-> assertThrows(Exception.class, ()-> validate("lastUpdateDate+P1D")),
                ()-> assertThrows(Exception.class, ()-> validate("ploc")),
                ()-> assertThrows(Exception.class, ()-> validate("lastUpdateTime+P15W"))
        );

    }

    @Test
    public void testValidDate() throws Exception {
        assertAll(
                ()-> assertDoesNotThrow(()-> validate("2021-04-12T23:20:50.52Z")),
                ()-> assertDoesNotThrow(()-> validate("lastUpdateTime+P1D")),
                ()-> assertDoesNotThrow(()-> validate("createTime+P90M")),
                ()-> assertDoesNotThrow(()-> validate("none"))
        );


    }
}
