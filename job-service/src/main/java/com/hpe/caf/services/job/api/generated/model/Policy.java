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


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

@ApiModel(
        description = "The expiration details to be applied on the job"
)
public class Policy {
    private String expiryTime = "createDate+10D";
    private ExpirationOperation expirationOperation = null;

    public Policy() {
    }

    @ApiModelProperty("")
    @JsonProperty("expiryTime")
    public String getExpiryTime() {
        return this.expiryTime;
    }

    public void setExpiryTime(String expiryTime) {
        this.expiryTime = expiryTime;
    }

    @ApiModelProperty("")
    @JsonProperty("expirationOperation")
    public ExpirationOperation getExpirationOperation() {
        return this.expirationOperation;
    }

    public void setExpirationOperation(ExpirationOperation expirationOperation) {
        this.expirationOperation = expirationOperation;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Policy policy = (Policy)o;
            return Objects.equals(this.expiryTime, policy.expiryTime) && Objects.equals(this.expirationOperation, policy.expirationOperation);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.expiryTime, this.expirationOperation});
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Policy {\n");
        sb.append("    expiryTime: ").append(this.toIndentedString(this.expiryTime)).append("\n");
        sb.append("    expirationOperation: ").append(this.toIndentedString(this.expirationOperation)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        return o == null ? "null" : o.toString().replace("\n", "\n    ");
    }
}
