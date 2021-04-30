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
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2021-03-12T12:28:51.784Z")
public class DeletePolicy {
    private String expiryTime = null;
    private DeletePolicy.OperationEnum operation;
    private Policer policer;

    public DeletePolicy() {
        this.operation = DeletePolicy.OperationEnum.DELETE;
        this.policer = null;
    }

    @ApiModelProperty("The delay before expiration")
    @JsonProperty("expiryTime")
    public String getExpiryTime() {
        return this.expiryTime;
    }

    public void setExpiryTime(String expiryTime) {
        this.expiryTime = expiryTime;
    }

    @ApiModelProperty("The action to apply on expired jobs")
    @JsonProperty("operation")
    public DeletePolicy.OperationEnum getOperation() {
        return this.operation;
    }

    public void setOperation(DeletePolicy.OperationEnum operation) {
        this.operation = operation;
    }

    @ApiModelProperty("The instance defining the policy")
    @JsonProperty("policer")
    public Policer getPolicer() {
        return this.policer;
    }

    public void setPolicer(Policer policer) {
        this.policer = policer;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            DeletePolicy deletePolicy = (DeletePolicy)o;
            return Objects.equals(this.expiryTime, deletePolicy.expiryTime) && Objects.equals(this.operation, deletePolicy.operation) && Objects.equals(this.policer, deletePolicy.policer);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.expiryTime, this.operation, this.policer});
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DeletePolicy {\n");
        sb.append("    expiryTime: ").append(this.toIndentedString(this.expiryTime)).append("\n");
        sb.append("    operation: ").append(this.toIndentedString(this.operation)).append("\n");
        sb.append("    policer: ").append(this.toIndentedString(this.policer)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        return o == null ? "null" : o.toString().replace("\n", "\n    ");
    }

    public static enum OperationEnum {
        DELETE("Delete");

        private String value;

        private OperationEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String toString() {
            return this.value;
        }
    }
}
