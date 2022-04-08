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
package com.hpe.caf.services.job.api.generated.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;



/**
 * The expiration details to be applied on the job
 **/

@ApiModel(description = "The expiration details to be applied on the job")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2021-03-16T14:04:09.471Z")
public class ExpirablePolicy {

    private String expiryTime = null;
    private OperationEnum operation = OperationEnum.EXPIRE;
    private Policer policer = null;



    /**
     * The delay before expiration
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("expiryTime")
    public String getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(String expiryTime) {
        this.expiryTime = expiryTime;
    }

    /**
     * The action to apply on expired jobs
     **/

    @ApiModelProperty(value = "The action to apply on expired jobs")
    @JsonProperty("operation")
    public OperationEnum getOperation() {
        return operation;
    }

    public void setOperation(OperationEnum operation) {
        this.operation = operation;
    }
    /**
     * The instance defining the policy
     **/

    @ApiModelProperty(value = "The instance defining the policy")
    @JsonProperty("policer")
    public Policer getPolicer() {
        return policer;
    }

    public void setPolicer(Policer policer) {
        this.policer = policer;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpirablePolicy expirablePolicy = (ExpirablePolicy) o;
        return Objects.equals(expiryTime, expirablePolicy.expiryTime) &&
            Objects.equals(operation, expirablePolicy.operation) &&
            Objects.equals(policer, expirablePolicy.policer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiryTime, operation, policer);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExpirablePolicy {\n");

        sb.append("    expiryTime: ").append(toIndentedString(expiryTime)).append("\n");
        sb.append("    operation: ").append(toIndentedString(operation)).append("\n");
        sb.append("    policer: ").append(toIndentedString(policer)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    public enum OperationEnum {
        EXPIRE("Expire"),
        DELETE("Delete");

        private String value;


        OperationEnum(String value) {
            this.value = value;
        }
        @Override
        @JsonValue
        public String toString() {
            return value;
        }

    }
}
